package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.mahasiswa.AnggotaKelompokItemDTO;
import com.unpar.scheduler.dto.mahasiswa.JadwalPresentasiMahasiswaDTO;
import com.unpar.scheduler.dto.mahasiswa.KelasBelumTerjadwalDTO;
import com.unpar.scheduler.dto.mahasiswa.MahasiswaJadwalResponseDTO;
import com.unpar.scheduler.model.entity.AnggotaKelompok;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kelompok;
import com.unpar.scheduler.model.entity.Mahasiswa;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.PesertaKelas;
import com.unpar.scheduler.model.entity.Presentasi;
import com.unpar.scheduler.repository.AnggotaKelompokRepository;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import com.unpar.scheduler.repository.PresentasiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// Menyusun daftar lengkap jadwal presentasi mahasiswa pada semester aktif beserta status dan anggota kelompoknya.
@Service
@Transactional(readOnly = true)
public class MahasiswaJadwalService {

    static final String KODE_HARI_INI = "HARI_INI";
    static final String KODE_AKAN_DATANG = "AKAN_DATANG";
    static final String KODE_SELESAI = "SELESAI";

    static final String LABEL_HARI_INI = "Hari Ini";
    static final String LABEL_AKAN_DATANG = "Akan Datang";
    static final String LABEL_SELESAI = "Selesai";

    private static final DateTimeFormatter JAM_MENIT_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] NAMA_HARI = {
        "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"
    };
    private static final String[] NAMA_BULAN = {
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    private static final Comparator<Presentasi> URUTAN_KRONOLOGIS = Comparator
        .comparing(Presentasi::getTanggal)
        .thenComparing(Presentasi::getJamMulai, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(Presentasi::getIdPresentasi, Comparator.nullsLast(Comparator.naturalOrder()));

    private final SemesterAkademikResolver semesterAkademikResolver;
    private final PesertaKelasRepository pesertaKelasRepository;
    private final PresentasiRepository presentasiRepository;
    private final AnggotaKelompokRepository anggotaKelompokRepository;
    private final NamaDosenPengajarResolver namaDosenPengajarResolver;

    public MahasiswaJadwalService(
        SemesterAkademikResolver semesterAkademikResolver,
        PesertaKelasRepository pesertaKelasRepository,
        PresentasiRepository presentasiRepository,
        AnggotaKelompokRepository anggotaKelompokRepository,
        NamaDosenPengajarResolver namaDosenPengajarResolver
    ) {
        this.semesterAkademikResolver = semesterAkademikResolver;
        this.pesertaKelasRepository = pesertaKelasRepository;
        this.presentasiRepository = presentasiRepository;
        this.anggotaKelompokRepository = anggotaKelompokRepository;
        this.namaDosenPengajarResolver = namaDosenPengajarResolver;
    }

    public MahasiswaJadwalResponseDTO getJadwal(Mahasiswa mahasiswa, LocalDate hariIni) {
        String npmBersih = mahasiswa.getNpm().trim();
        Optional<String> semesterOpt = semesterAkademikResolver.resolveActiveSemesterForMahasiswa(npmBersih);
        String semesterLabel = SemesterAkademikResolver.formatSemesterLabel(semesterOpt.orElse(null));
        if (semesterOpt.isEmpty()) {
            return new MahasiswaJadwalResponseDTO(mahasiswa.getNama(), npmBersih, semesterLabel, List.of(), List.of());
        }

        String semester = semesterOpt.get();
        List<PesertaKelas> pesertaList = pesertaKelasRepository.findByNpmAndSemesterWithDetails(mahasiswa.getNpm(), semester);
        if (pesertaList.isEmpty()) {
            return new MahasiswaJadwalResponseDTO(mahasiswa.getNama(), npmBersih, semesterLabel, List.of(), List.of());
        }

        List<Presentasi> presentasiTerurut = presentasiRepository
            .findByMahasiswaNpmAndSemester(mahasiswa.getNpm(), semester).stream()
            .sorted(URUTAN_KRONOLOGIS)
            .toList();

        List<JadwalPresentasiMahasiswaDTO> daftarJadwal = susunDaftarJadwal(presentasiTerurut, npmBersih, hariIni);
        List<KelasBelumTerjadwalDTO> belumTerjadwal = susunKelasBelumTerjadwal(
            pesertaList, presentasiTerurut, mahasiswa.getNpm(), semester
        );

        return new MahasiswaJadwalResponseDTO(
            mahasiswa.getNama(), npmBersih, semesterLabel, daftarJadwal, belumTerjadwal
        );
    }

    private List<JadwalPresentasiMahasiswaDTO> susunDaftarJadwal(
        List<Presentasi> presentasiList,
        String npmAktif,
        LocalDate hariIni
    ) {
        if (presentasiList.isEmpty()) {
            return List.of();
        }

        List<Kelas> kelasTerjadwal = presentasiList.stream()
            .map(p -> p.getKelompok().getKelas())
            .toList();
        Map<Integer, String> dosenPerKelas = namaDosenPengajarResolver.resolve(kelasTerjadwal);

        List<Integer> kelompokIds = presentasiList.stream()
            .map(p -> p.getKelompok().getIdKelompok())
            .toList();
        Map<Integer, List<AnggotaKelompok>> anggotaByKelompok = anggotaKelompokRepository
            .findByKelompokIdsWithMahasiswa(kelompokIds).stream()
            .collect(Collectors.groupingBy(ak -> ak.getKelompok().getIdKelompok()));

        return presentasiList.stream()
            .map(p -> toJadwalDto(p, npmAktif, hariIni, dosenPerKelas, anggotaByKelompok))
            .toList();
    }

    private JadwalPresentasiMahasiswaDTO toJadwalDto(
        Presentasi presentasi,
        String npmAktif,
        LocalDate hariIni,
        Map<Integer, String> dosenPerKelas,
        Map<Integer, List<AnggotaKelompok>> anggotaByKelompok
    ) {
        Kelompok kelompok = presentasi.getKelompok();
        Kelas kelas = kelompok.getKelas();
        MataKuliah mk = kelas.getMataKuliahBuka().getMatakuliah();
        LocalDate tanggal = presentasi.getTanggal();

        String waktuMulai = formatJam(presentasi.getJamMulai());
        String waktuSelesai = formatJam(presentasi.getJamSelesai());
        String statusKode = tentukanStatusKode(tanggal, hariIni);

        List<AnggotaKelompokItemDTO> daftarAnggota = anggotaByKelompok
            .getOrDefault(kelompok.getIdKelompok(), List.of()).stream()
            .map(ak -> {
                String npmAnggota = ak.getMahasiswa().getNpm().trim();
                return new AnggotaKelompokItemDTO(npmAnggota, ak.getMahasiswa().getNama(), npmAnggota.equals(npmAktif));
            })
            .sorted(Comparator
                .comparing(AnggotaKelompokItemDTO::isCurrentUser).reversed()
                .thenComparing(AnggotaKelompokItemDTO::nama, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AnggotaKelompokItemDTO::npm))
            .toList();

        return new JadwalPresentasiMahasiswaDTO(
            presentasi.getIdPresentasi(),
            kelompok.getIdKelompok(),
            kelompok.getNoKelompok(),
            bersihkanNullable(kelompok.getTopik()),
            mk.getIdMatkul(),
            mk.getNama(),
            kelas.getKodeKelas(),
            dosenPerKelas.getOrDefault(kelas.getIdKelas(), NamaDosenPengajarResolver.TANPA_DOSEN),
            tanggal.toString(),
            formatHariTanggal(tanggal),
            String.format("%02d", tanggal.getDayOfMonth()),
            formatBulanTahunUppercase(tanggal),
            waktuMulai,
            waktuSelesai,
            waktuMulai + " - " + waktuSelesai + " WIB",
            bersihkanNullable(presentasi.getRuangan()),
            bersihkanNullable(presentasi.getCatatan()),
            statusKode,
            tentukanStatusLabel(statusKode),
            daftarAnggota
        );
    }

    private List<KelasBelumTerjadwalDTO> susunKelasBelumTerjadwal(
        List<PesertaKelas> pesertaList,
        List<Presentasi> presentasiList,
        String npmRaw,
        String semester
    ) {
        Set<Integer> idKelasTerjadwal = new HashSet<>();
        for (Presentasi p : presentasiList) {
            idKelasTerjadwal.add(p.getKelompok().getKelas().getIdKelas());
        }

        Map<Integer, Kelompok> kelompokByKelas = new HashMap<>();
        for (AnggotaKelompok ak : anggotaKelompokRepository.findByMahasiswaNpmInWithDetails(List.of(npmRaw))) {
            if (semester.equals(ak.getKelas().getMataKuliahBuka().getSemesterAkademik().trim())) {
                kelompokByKelas.put(ak.getKelas().getIdKelas(), ak.getKelompok());
            }
        }

        return pesertaList.stream()
            .map(PesertaKelas::getKelas)
            .filter(kelas -> !idKelasTerjadwal.contains(kelas.getIdKelas()))
            .sorted(Comparator
                .comparing((Kelas k) -> k.getMataKuliahBuka().getMatakuliah().getIdMatkul())
                .thenComparing(Kelas::getKodeKelas))
            .map(kelas -> {
                MataKuliah mk = kelas.getMataKuliahBuka().getMatakuliah();
                Kelompok kel = kelompokByKelas.get(kelas.getIdKelas());
                return new KelasBelumTerjadwalDTO(
                    kelas.getIdKelas(),
                    mk.getIdMatkul(),
                    mk.getNama(),
                    kelas.getKodeKelas(),
                    kel != null,
                    kel != null ? kel.getNoKelompok() : null
                );
            })
            .toList();
    }

    static String formatHariTanggal(LocalDate tanggal) {
        String hari = NAMA_HARI[tanggal.getDayOfWeek().getValue() - 1];
        String bulan = NAMA_BULAN[tanggal.getMonthValue() - 1];
        return hari + ", " + tanggal.getDayOfMonth() + " " + bulan + " " + tanggal.getYear();
    }

    static String formatBulanTahunUppercase(LocalDate tanggal) {
        String bulan = NAMA_BULAN[tanggal.getMonthValue() - 1].toUpperCase(Locale.ROOT);
        return bulan + " " + tanggal.getYear();
    }

    private static String formatJam(LocalTime waktu) {
        return waktu != null ? waktu.format(JAM_MENIT_FMT) : "-";
    }

    private static String tentukanStatusKode(LocalDate tanggal, LocalDate hariIni) {
        if (tanggal.isEqual(hariIni)) {
            return KODE_HARI_INI;
        }
        return tanggal.isAfter(hariIni) ? KODE_AKAN_DATANG : KODE_SELESAI;
    }

    private static String tentukanStatusLabel(String statusKode) {
        return switch (statusKode) {
            case KODE_HARI_INI -> LABEL_HARI_INI;
            case KODE_AKAN_DATANG -> LABEL_AKAN_DATANG;
            default -> LABEL_SELESAI;
        };
    }

    private static String bersihkanNullable(String nilai) {
        if (nilai == null || nilai.isBlank()) {
            return null;
        }
        return nilai.trim();
    }
}
