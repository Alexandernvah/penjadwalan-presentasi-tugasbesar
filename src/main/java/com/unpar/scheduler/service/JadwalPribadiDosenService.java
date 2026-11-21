package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.jadwalpribadi.JadwalPresentasiItemDTO;
import com.unpar.scheduler.dto.jadwalpribadi.JadwalPribadiDosenResponseDTO;
import com.unpar.scheduler.dto.jadwalpribadi.JadwalRutinItemDTO;
import com.unpar.scheduler.dto.jadwalpribadi.JadwalUjianItemDTO;
import com.unpar.scheduler.dto.jadwalpribadi.MahasiswaSederhanaDTO;
import com.unpar.scheduler.dto.penjadwalan.AnggotaPenjadwalanDTO;
import com.unpar.scheduler.dto.penjadwalan.KelompokPenjadwalanDTO;
import com.unpar.scheduler.model.entity.AnggotaKelompok;
import com.unpar.scheduler.model.entity.Dosen;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kelompok;
import com.unpar.scheduler.model.entity.Kuliah;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.PengawasUjian;
import com.unpar.scheduler.model.entity.PesertaKelas;
import com.unpar.scheduler.model.entity.Presentasi;
import com.unpar.scheduler.model.entity.Ujian;
import com.unpar.scheduler.repository.AnggotaKelompokRepository;
import com.unpar.scheduler.repository.DosenRepository;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KuliahRepository;
import com.unpar.scheduler.repository.PengawasUjianRepository;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import com.unpar.scheduler.repository.PresentasiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Menyusun agenda pribadi dosen: jadwal mengajar rutin, pengawasan ujian, dan
 * presentasi tugas besar. Bila dosen atau kelasnya belum ada di database, data
 * contoh yang konsisten dengan modul dashboard dan penjadwalan dipakai.
 */
@Service
@Transactional(readOnly = true)
public class JadwalPribadiDosenService {

    private static final String DEFAULT_NIDN = "1975031501";
    private static final String DEFAULT_SEMESTER = "25261";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] NAMA_HARI = {
        "", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"
    };

    private static final int FALLBACK_KELAS_JARKOM = 102;
    private static final int FALLBACK_KELAS_ASD = 101;

    private static final List<String> FALLBACK_NAMA_JARKOM = List.of(
        "Andi Saputra", "Nurul Hidayah", "Rizky Maulana", "Salsabila Putri",
        "Bagas Prasetyo", "Citra Maharani", "Dewi Anggraini", "Eko Nugroho",
        "Fajar Ramadhan", "Gilang Permana", "Hana Safitri", "Iqbal Firmansyah",
        "Jessica Tan", "Kevin Santoso", "Laras Wulandari", "Michael Gunawan",
        "Nadia Kusumawati", "Oscar Halim", "Putri Ayu Lestari", "Qori Rahman",
        "Rafi Aditya", "Sinta Marlina", "Tommy Wijaya", "Vina Oktaviani"
    );

    private static final List<String> FALLBACK_NAMA_ASD = List.of(
        "Dimas Pratama", "Evelyn Wijaya", "Farhan Akbar", "Gita Permata",
        "Hendra Kusuma", "Indah Lestari", "Joko Susilo", "Kartika Sari",
        "Lukas Hartono", "Maya Puspita", "Naufal Hakim", "Olivia Chandra",
        "Panji Wicaksono", "Queenie Salim", "Reza Pahlevi", "Stefanus Gunadi",
        "Tiara Anindya", "Umar Bakri", "Valerie Santika", "Wahyu Setiawan"
    );

    private final DosenRepository dosenRepository;
    private final KelasRepository kelasRepository;
    private final KuliahRepository kuliahRepository;
    private final PesertaKelasRepository pesertaKelasRepository;
    private final PengawasUjianRepository pengawasUjianRepository;
    private final PresentasiRepository presentasiRepository;
    private final AnggotaKelompokRepository anggotaKelompokRepository;
    private final PenjadwalanService penjadwalanService;

    public JadwalPribadiDosenService(
        DosenRepository dosenRepository,
        KelasRepository kelasRepository,
        KuliahRepository kuliahRepository,
        PesertaKelasRepository pesertaKelasRepository,
        PengawasUjianRepository pengawasUjianRepository,
        PresentasiRepository presentasiRepository,
        AnggotaKelompokRepository anggotaKelompokRepository,
        PenjadwalanService penjadwalanService
    ) {
        this.dosenRepository = dosenRepository;
        this.kelasRepository = kelasRepository;
        this.kuliahRepository = kuliahRepository;
        this.pesertaKelasRepository = pesertaKelasRepository;
        this.pengawasUjianRepository = pengawasUjianRepository;
        this.presentasiRepository = presentasiRepository;
        this.anggotaKelompokRepository = anggotaKelompokRepository;
        this.penjadwalanService = penjadwalanService;
    }

    public JadwalPribadiDosenResponseDTO getJadwalPribadi(String nidn) {
        String targetNidn = (nidn != null && !nidn.trim().isEmpty()) ? nidn.trim() : DEFAULT_NIDN;

        Optional<Dosen> dosenOpt = dosenRepository.findByNomorIndukDosen(targetNidn);
        if (dosenOpt.isEmpty()) {
            return buildFallback();
        }

        String nidnDosen = dosenOpt.get().getNomorIndukDosen();
        List<Kelas> classes = new ArrayList<>(
            kelasRepository.findClassesByLecturerAndSemester(nidnDosen, DEFAULT_SEMESTER)
        );
        if (classes.isEmpty()) {
            return buildFallback();
        }

        classes.sort(
            Comparator.comparing((Kelas k) -> resolveKodeMatkul(k))
                .thenComparing(k -> k.getKodeKelas() != null ? k.getKodeKelas() : "")
        );
        List<Integer> kelasIds = classes.stream().map(Kelas::getIdKelas).toList();

        return new JadwalPribadiDosenResponseDTO(
            buildJadwalRutin(nidnDosen, classes, kelasIds),
            buildJadwalUjian(nidnDosen),
            buildJadwalPresentasi(kelasIds)
        );
    }

    // ---------------------------------------------------------------------
    // Jadwal rutin
    // ---------------------------------------------------------------------

    private List<JadwalRutinItemDTO> buildJadwalRutin(String nidn, List<Kelas> classes, List<Integer> kelasIds) {
        Map<Integer, List<MahasiswaSederhanaDTO>> studentsByKelas = loadStudentsByKelas(kelasIds);

        List<JadwalRutinItemDTO> result = new ArrayList<>();
        for (Kelas kelas : classes) {
            List<Kuliah> kuliahList = new ArrayList<>(
                kuliahRepository.findByKelas_IdKelasAndDosenPengajar_NomorIndukDosen(kelas.getIdKelas(), nidn)
            );
            kuliahList.sort(
                Comparator.comparing((Kuliah k) -> k.getHari() != null ? k.getHari() : 0)
                    .thenComparing(Kuliah::getJamMulai, Comparator.nullsLast(Comparator.naturalOrder()))
            );

            List<MahasiswaSederhanaDTO> students = studentsByKelas.getOrDefault(
                kelas.getIdKelas(), Collections.emptyList()
            );
            MataKuliah mk = kelas.getMataKuliahBuka().getMatakuliah();

            result.add(new JadwalRutinItemDTO(
                kelas.getIdKelas(),
                mk.getIdMatkul(),
                mk.getNama(),
                "Kelas " + kelas.getKodeKelas(),
                buildJadwalRingkas(kuliahList),
                buildRuangan(kuliahList),
                students.size(),
                students
            ));
        }
        return result;
    }

    private Map<Integer, List<MahasiswaSederhanaDTO>> loadStudentsByKelas(List<Integer> kelasIds) {
        Map<Integer, Map<String, MahasiswaSederhanaDTO>> grouped = new HashMap<>();
        for (PesertaKelas pk : pesertaKelasRepository.findByKelasIdsWithMahasiswa(kelasIds)) {
            if (pk.getKelas() == null || pk.getMahasiswa() == null) {
                continue;
            }
            String npm = pk.getMahasiswa().getNpm();
            grouped
                .computeIfAbsent(pk.getKelas().getIdKelas(), id -> new TreeMap<>())
                .put(npm, new MahasiswaSederhanaDTO(npm, pk.getMahasiswa().getNama()));
        }

        Map<Integer, List<MahasiswaSederhanaDTO>> result = new HashMap<>();
        grouped.forEach((idKelas, byNpm) -> result.put(idKelas, List.copyOf(byNpm.values())));
        return result;
    }

    static String buildJadwalRingkas(List<Kuliah> kuliahList) {
        Map<String, TreeSet<Integer>> daysByTime = new LinkedHashMap<>();
        for (Kuliah kuliah : kuliahList) {
            if (kuliah.getHari() == null || kuliah.getJamMulai() == null || kuliah.getJamSelesai() == null) {
                continue;
            }
            String timeKey = formatTime(kuliah.getJamMulai()) + " - " + formatTime(kuliah.getJamSelesai());
            daysByTime.computeIfAbsent(timeKey, k -> new TreeSet<>()).add(kuliah.getHari().intValue());
        }
        if (daysByTime.isEmpty()) {
            return "Jadwal belum ditetapkan";
        }

        List<Map.Entry<String, TreeSet<Integer>>> entries = new ArrayList<>(daysByTime.entrySet());
        entries.sort(
            Comparator.comparing((Map.Entry<String, TreeSet<Integer>> e) -> e.getValue().first())
                .thenComparing(Map.Entry::getKey)
        );

        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, TreeSet<Integer>> entry : entries) {
            List<String> dayNames = new ArrayList<>();
            for (Integer day : entry.getValue()) {
                dayNames.add(namaHari(day));
            }
            parts.add(String.join(", ", dayNames) + " (" + entry.getKey() + ")");
        }
        return String.join("; ", parts);
    }

    static String buildRuangan(List<Kuliah> kuliahList) {
        Set<String> rooms = new LinkedHashSet<>();
        for (Kuliah kuliah : kuliahList) {
            String room = kuliah.getRuangan();
            if (room != null && !room.isBlank() && !"-".equals(room.trim())) {
                rooms.add(room.trim());
            }
        }
        return rooms.isEmpty() ? "-" : String.join(", ", rooms);
    }

    private String resolveKodeMatkul(Kelas kelas) {
        return kelas.getMataKuliahBuka().getMatakuliah().getIdMatkul();
    }

    // ---------------------------------------------------------------------
    // Jadwal ujian
    // ---------------------------------------------------------------------

    private List<JadwalUjianItemDTO> buildJadwalUjian(String nidn) {
        List<JadwalUjianItemDTO> result = new ArrayList<>();
        for (PengawasUjian pengawas : pengawasUjianRepository.findByDosenAndSemesterWithDetails(nidn, DEFAULT_SEMESTER)) {
            Ujian ujian = pengawas.getUjian();
            if (ujian == null) {
                continue;
            }
            MataKuliah mk = ujian.getMataKuliahBuka().getMatakuliah();
            result.add(new JadwalUjianItemDTO(
                ujian.getIdUjian(),
                ujian.getJenisUjian(),
                ujian.getTanggal().toString(),
                formatTime(ujian.getJamMulai()),
                formatTime(ujian.getJamSelesai()),
                mk.getIdMatkul(),
                mk.getNama(),
                ujian.getRuangan()
            ));
        }
        result.sort(
            Comparator.comparing(JadwalUjianItemDTO::tanggal)
                .thenComparing(JadwalUjianItemDTO::waktuMulai)
        );
        return result;
    }

    // ---------------------------------------------------------------------
    // Jadwal presentasi
    // ---------------------------------------------------------------------

    private List<JadwalPresentasiItemDTO> buildJadwalPresentasi(List<Integer> kelasIds) {
        List<Presentasi> presentasiList = presentasiRepository.findByKelasIdsWithDetails(kelasIds);
        if (presentasiList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> kelompokIds = presentasiList.stream()
            .map(p -> p.getKelompok().getIdKelompok())
            .distinct()
            .toList();

        Map<Integer, List<MahasiswaSederhanaDTO>> anggotaByKelompok = new HashMap<>();
        for (AnggotaKelompok ak : anggotaKelompokRepository.findByKelompokIdsWithMahasiswa(kelompokIds)) {
            if (ak.getKelompok() == null || ak.getMahasiswa() == null) {
                continue;
            }
            anggotaByKelompok
                .computeIfAbsent(ak.getKelompok().getIdKelompok(), id -> new ArrayList<>())
                .add(new MahasiswaSederhanaDTO(ak.getMahasiswa().getNpm(), ak.getMahasiswa().getNama()));
        }

        List<JadwalPresentasiItemDTO> result = new ArrayList<>();
        for (Presentasi p : presentasiList) {
            Kelompok kelompok = p.getKelompok();
            MataKuliah mk = kelompok.getKelas().getMataKuliahBuka().getMatakuliah();
            List<MahasiswaSederhanaDTO> anggota = new ArrayList<>(
                anggotaByKelompok.getOrDefault(kelompok.getIdKelompok(), Collections.emptyList())
            );
            anggota.sort(Comparator.comparing(MahasiswaSederhanaDTO::npm));

            result.add(new JadwalPresentasiItemDTO(
                p.getIdPresentasi(),
                p.getTanggal().toString(),
                formatTime(p.getJamMulai()),
                formatTime(p.getJamSelesai()),
                mk.getIdMatkul(),
                mk.getNama(),
                "Kelompok " + kelompok.getNoKelompok(),
                kelompok.getTopik(),
                p.getRuangan(),
                anggota
            ));
        }
        sortPresentasi(result);
        return result;
    }

    private static void sortPresentasi(List<JadwalPresentasiItemDTO> list) {
        list.sort(
            Comparator.comparing(JadwalPresentasiItemDTO::tanggal)
                .thenComparing(JadwalPresentasiItemDTO::waktuMulai)
        );
    }

    // ---------------------------------------------------------------------
    // Data contoh bila database belum terisi
    // ---------------------------------------------------------------------

    private JadwalPribadiDosenResponseDTO buildFallback() {
        List<MahasiswaSederhanaDTO> jarkomStudents = buildFallbackStudents("61821010", FALLBACK_NAMA_JARKOM);
        List<MahasiswaSederhanaDTO> asdStudents = buildFallbackStudents("61822010", FALLBACK_NAMA_ASD);

        List<JadwalRutinItemDTO> rutin = List.of(
            new JadwalRutinItemDTO(
                FALLBACK_KELAS_JARKOM, "IF3130", "Jaringan Komputer", "Kelas B",
                "Senin, Rabu, Jumat (13:00 - 15:00)", "Ruang 9013",
                jarkomStudents.size(), jarkomStudents
            ),
            new JadwalRutinItemDTO(
                FALLBACK_KELAS_ASD, "IF2110", "Algoritma dan Struktur Data", "Kelas A",
                "Selasa, Kamis (08:00 - 10:00)", "Lab Komputer 1",
                asdStudents.size(), asdStudents
            )
        );

        List<JadwalUjianItemDTO> ujian = List.of(
            new JadwalUjianItemDTO(1, "UTS", "2026-10-19", "08:00", "10:00",
                "IF1110", "Algoritma Pemrograman", "Ruang 9012"),
            new JadwalUjianItemDTO(2, "UTS", "2026-10-21", "13:00", "15:00",
                "IF3130", "Jaringan Komputer", "Ruang 9013"),
            new JadwalUjianItemDTO(3, "UAS", "2026-12-09", "10:00", "12:00",
                "IF2110", "Algoritma dan Struktur Data", "Lab Komputer 1")
        );

        return new JadwalPribadiDosenResponseDTO(rutin, ujian, buildFallbackPresentasi(rutin));
    }

    /**
     * Presentasi yang dijadwalkan lewat modul penjadwalan (mode data contoh)
     * ditampilkan apa adanya; contoh statis hanya muncul bila belum ada satu pun.
     */
    private List<JadwalPresentasiItemDTO> buildFallbackPresentasi(List<JadwalRutinItemDTO> rutin) {
        List<JadwalPresentasiItemDTO> result = new ArrayList<>();
        for (JadwalRutinItemDTO kelas : rutin) {
            for (KelompokPenjadwalanDTO kelompok : penjadwalanService.getKelompokByKelas(kelas.idKelas())) {
                if (!Boolean.TRUE.equals(kelompok.terjadwal()) || kelompok.tanggal() == null) {
                    continue;
                }
                List<MahasiswaSederhanaDTO> anggota = new ArrayList<>();
                if (kelompok.anggota() != null) {
                    for (AnggotaPenjadwalanDTO a : kelompok.anggota()) {
                        anggota.add(new MahasiswaSederhanaDTO(a.npm(), a.nama()));
                    }
                }
                anggota.sort(Comparator.comparing(MahasiswaSederhanaDTO::npm));

                result.add(new JadwalPresentasiItemDTO(
                    kelompok.idPresentasi(),
                    kelompok.tanggal(),
                    kelompok.jamMulai(),
                    kelompok.jamSelesai(),
                    kelas.kodeMatkul(),
                    kelas.namaMatkul(),
                    "Kelompok " + kelompok.noKelompok(),
                    kelompok.topik(),
                    kelompok.ruangan(),
                    anggota
                ));
            }
        }

        if (result.isEmpty()) {
            result.add(new JadwalPresentasiItemDTO(
                1, "2026-10-06", "09:00", "09:30",
                "IF3130", "Jaringan Komputer", "Kelompok 2",
                "Perancangan Topologi Jaringan Kampus", "Ruang 9122",
                List.of(
                    new MahasiswaSederhanaDTO("6182101001", "Andi Saputra"),
                    new MahasiswaSederhanaDTO("6182101002", "Nurul Hidayah"),
                    new MahasiswaSederhanaDTO("6182101003", "Rizky Maulana"),
                    new MahasiswaSederhanaDTO("6182101004", "Salsabila Putri")
                )
            ));
        }
        sortPresentasi(result);
        return result;
    }

    private static List<MahasiswaSederhanaDTO> buildFallbackStudents(String npmPrefix, List<String> names) {
        List<MahasiswaSederhanaDTO> students = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            students.add(new MahasiswaSederhanaDTO(npmPrefix + String.format("%02d", i + 1), names.get(i)));
        }
        return students;
    }

    // ---------------------------------------------------------------------
    // Util
    // ---------------------------------------------------------------------

    private static String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FMT) : "";
    }

    private static String namaHari(int hari) {
        return hari >= 1 && hari <= 7 ? NAMA_HARI[hari] : "-";
    }
}
