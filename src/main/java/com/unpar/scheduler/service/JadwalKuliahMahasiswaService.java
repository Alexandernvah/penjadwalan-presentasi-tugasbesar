package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.jadwalkuliah.JadwalKuliahActionResponseDTO;
import com.unpar.scheduler.dto.jadwalkuliah.JadwalKuliahSayaItemDTO;
import com.unpar.scheduler.dto.jadwalkuliah.KatalogJadwalKuliahResponseDTO;
import com.unpar.scheduler.dto.jadwalkuliah.KelasPilihanDTO;
import com.unpar.scheduler.dto.jadwalkuliah.MataKuliahPilihanDTO;
import com.unpar.scheduler.dto.jadwalkuliah.PilihKelasRequestDTO;
import com.unpar.scheduler.dto.jadwalkuliah.SlotKuliahDTO;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kuliah;
import com.unpar.scheduler.model.entity.Mahasiswa;
import com.unpar.scheduler.model.entity.MataKuliahBuka;
import com.unpar.scheduler.model.entity.PesertaKelas;
import com.unpar.scheduler.repository.AnggotaKelompokRepository;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KuliahRepository;
import com.unpar.scheduler.repository.MahasiswaRepository;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Jadwal kuliah reguler mahasiswa. Mata kuliah, kelas, dan jadwalnya (hari, jam,
 * ruangan) sudah ditetapkan admin di database, sehingga mahasiswa hanya memilih
 * kelas yang diambil pada semester berjalan (semester akademik terbaru).
 */
@Service
@Transactional(readOnly = true)
public class JadwalKuliahMahasiswaService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] NAMA_HARI = {
        "", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"
    };

    private final MahasiswaRepository mahasiswaRepository;
    private final KelasRepository kelasRepository;
    private final KuliahRepository kuliahRepository;
    private final PesertaKelasRepository pesertaKelasRepository;
    private final AnggotaKelompokRepository anggotaKelompokRepository;

    public JadwalKuliahMahasiswaService(
        MahasiswaRepository mahasiswaRepository,
        KelasRepository kelasRepository,
        KuliahRepository kuliahRepository,
        PesertaKelasRepository pesertaKelasRepository,
        AnggotaKelompokRepository anggotaKelompokRepository
    ) {
        this.mahasiswaRepository = mahasiswaRepository;
        this.kelasRepository = kelasRepository;
        this.kuliahRepository = kuliahRepository;
        this.pesertaKelasRepository = pesertaKelasRepository;
        this.anggotaKelompokRepository = anggotaKelompokRepository;
    }

    /** Daftar mata kuliah semester berjalan beserta kelas dan jadwalnya. */
    public KatalogJadwalKuliahResponseDTO getKatalog(String npm) {
        String semester = semesterBerjalan();
        if (semester == null) {
            return new KatalogJadwalKuliahResponseDTO(null, List.of());
        }

        List<Kelas> semuaKelas = kelasRepository.findBySemesterWithDetails(semester);
        Map<Integer, List<Kuliah>> kuliahPerKelas = kuliahPerKelas(semuaKelas);

        Map<Integer, Integer> kelasTerdaftarPerMatkul = new HashMap<>();
        Set<Integer> idKelasTerdaftar = new HashSet<>();
        for (PesertaKelas pk : pesertaKelasRepository.findByNpmAndSemesterWithDetails(npm, semester)) {
            idKelasTerdaftar.add(pk.getKelas().getIdKelas());
            kelasTerdaftarPerMatkul.put(
                pk.getKelas().getMataKuliahBuka().getIdMatkulBuka(),
                pk.getKelas().getIdKelas()
            );
        }

        Map<Integer, List<Kelas>> kelasPerMatkulBuka = new LinkedHashMap<>();
        for (Kelas kelas : semuaKelas) {
            kelasPerMatkulBuka
                .computeIfAbsent(kelas.getMataKuliahBuka().getIdMatkulBuka(), k -> new ArrayList<>())
                .add(kelas);
        }

        List<MataKuliahPilihanDTO> hasil = new ArrayList<>();
        for (List<Kelas> kelasGroup : kelasPerMatkulBuka.values()) {
            MataKuliahBuka buka = kelasGroup.get(0).getMataKuliahBuka();
            List<KelasPilihanDTO> kelasDto = new ArrayList<>();
            for (Kelas kelas : kelasGroup) {
                kelasDto.add(new KelasPilihanDTO(
                    kelas.getIdKelas(),
                    kelas.getKodeKelas(),
                    namaPenanggungJawab(kelas),
                    toSlots(kuliahPerKelas.get(kelas.getIdKelas())),
                    idKelasTerdaftar.contains(kelas.getIdKelas())
                ));
            }
            hasil.add(new MataKuliahPilihanDTO(
                buka.getIdMatkulBuka(),
                buka.getMatakuliah().getIdMatkul(),
                buka.getMatakuliah().getNama(),
                buka.getMatakuliah().getSks(),
                kelasTerdaftarPerMatkul.get(buka.getIdMatkulBuka()),
                kelasDto
            ));
        }
        return new KatalogJadwalKuliahResponseDTO(semester, hasil);
    }

    /** Kelas yang sudah diambil mahasiswa pada semester berjalan. */
    public List<JadwalKuliahSayaItemDTO> getJadwalSaya(String npm) {
        String semester = semesterBerjalan();
        if (semester == null) {
            return List.of();
        }

        List<PesertaKelas> peserta = pesertaKelasRepository.findByNpmAndSemesterWithDetails(npm, semester);
        List<Kelas> kelasList = peserta.stream().map(PesertaKelas::getKelas).toList();
        Map<Integer, List<Kuliah>> kuliahPerKelas = kuliahPerKelas(kelasList);

        List<JadwalKuliahSayaItemDTO> hasil = new ArrayList<>();
        for (Kelas kelas : kelasList) {
            MataKuliahBuka buka = kelas.getMataKuliahBuka();
            hasil.add(new JadwalKuliahSayaItemDTO(
                kelas.getIdKelas(),
                buka.getMatakuliah().getIdMatkul(),
                buka.getMatakuliah().getNama(),
                buka.getMatakuliah().getSks(),
                kelas.getKodeKelas(),
                namaPenanggungJawab(kelas),
                toSlots(kuliahPerKelas.get(kelas.getIdKelas()))
            ));
        }
        hasil.sort(Comparator
            .comparing((JadwalKuliahSayaItemDTO item) -> item.jadwal().isEmpty() ? Integer.MAX_VALUE : item.jadwal().get(0).hari())
            .thenComparing(item -> item.jadwal().isEmpty() ? "" : item.jadwal().get(0).jamMulai())
            .thenComparing(JadwalKuliahSayaItemDTO::namaMatkul));
        return hasil;
    }

    @Transactional
    public JadwalKuliahActionResponseDTO tambahKelas(String npm, PilihKelasRequestDTO request) {
        if (request == null || request.idKelas() == null) {
            throw new IllegalArgumentException("Pilih mata kuliah dan kelas terlebih dahulu.");
        }
        Mahasiswa mahasiswa = mahasiswaRepository.findByNpm(npm)
            .orElseThrow(() -> new IllegalArgumentException("Data mahasiswa tidak ditemukan."));
        Kelas kelas = kelasRepository.findByIdWithDetails(request.idKelas())
            .orElseThrow(() -> new IllegalArgumentException("Kelas yang dipilih tidak ditemukan."));

        String semester = semesterBerjalan();
        MataKuliahBuka buka = kelas.getMataKuliahBuka();
        if (semester == null || !semester.equals(buka.getSemesterAkademik())) {
            throw new IllegalArgumentException("Kelas yang dipilih bukan bagian dari semester berjalan.");
        }

        String namaKelas = buka.getMatakuliah().getNama() + " Kelas " + kelas.getKodeKelas();

        if (pesertaKelasRepository.findByMahasiswa_NpmAndKelas_IdKelas(npm, kelas.getIdKelas()).isPresent()) {
            return new JadwalKuliahActionResponseDTO(false, "Anda sudah terdaftar di " + namaKelas + ".");
        }
        for (PesertaKelas pk : pesertaKelasRepository.findByNpmAndSemesterWithDetails(npm, semester)) {
            Kelas existing = pk.getKelas();
            if (existing.getMataKuliahBuka().getIdMatkulBuka().equals(buka.getIdMatkulBuka())) {
                return new JadwalKuliahActionResponseDTO(false,
                    "Anda sudah terdaftar di Kelas " + existing.getKodeKelas() + " untuk mata kuliah "
                        + buka.getMatakuliah().getNama() + ". Hapus kelas tersebut dahulu jika ingin pindah kelas.");
            }
        }

        pesertaKelasRepository.save(new PesertaKelas(null, mahasiswa, kelas));
        return new JadwalKuliahActionResponseDTO(true, namaKelas + " berhasil ditambahkan ke jadwal Anda.");
    }

    @Transactional
    public JadwalKuliahActionResponseDTO hapusKelas(String npm, Integer idKelas) {
        if (idKelas == null) {
            throw new IllegalArgumentException("Kelas yang akan dihapus tidak valid.");
        }
        Optional<PesertaKelas> peserta = pesertaKelasRepository.findByMahasiswa_NpmAndKelas_IdKelas(npm, idKelas);
        if (peserta.isEmpty()) {
            throw new IllegalArgumentException("Kelas tersebut tidak ada di jadwal Anda.");
        }
        if (anggotaKelompokRepository.existsByMahasiswa_NpmAndKelas_IdKelas(npm, idKelas)) {
            return new JadwalKuliahActionResponseDTO(false,
                "Kelas ini tidak dapat dihapus karena Anda sudah menjadi anggota kelompok tugas besar di kelas tersebut.");
        }
        pesertaKelasRepository.delete(peserta.get());
        return new JadwalKuliahActionResponseDTO(true, "Kelas berhasil dihapus dari jadwal Anda.");
    }

    private String semesterBerjalan() {
        return kelasRepository.findLatestSemesterOverall().orElse(null);
    }

    private Map<Integer, List<Kuliah>> kuliahPerKelas(List<Kelas> kelasList) {
        if (kelasList.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Integer> ids = kelasList.stream().map(Kelas::getIdKelas).toList();
        Map<Integer, List<Kuliah>> hasil = new HashMap<>();
        for (Kuliah kuliah : kuliahRepository.findByKelasIdsWithDetails(ids)) {
            hasil.computeIfAbsent(kuliah.getKelas().getIdKelas(), k -> new ArrayList<>()).add(kuliah);
        }
        return hasil;
    }

    private static List<SlotKuliahDTO> toSlots(List<Kuliah> kuliahList) {
        if (kuliahList == null || kuliahList.isEmpty()) {
            return List.of();
        }
        return kuliahList.stream()
            .sorted(Comparator.comparing(Kuliah::getHari).thenComparing(Kuliah::getJamMulai))
            .map(kuliah -> new SlotKuliahDTO(
                kuliah.getHari().intValue(),
                namaHari(kuliah.getHari()),
                kuliah.getJamMulai().format(TIME_FMT),
                kuliah.getJamSelesai().format(TIME_FMT),
                kuliah.getRuangan(),
                kuliah.getDosenPengajar().getNama(),
                kuliah.getTipePertemuan()
            ))
            .toList();
    }

    private static String namaHari(Short hari) {
        int idx = hari == null ? 0 : hari;
        return idx >= 1 && idx < NAMA_HARI.length ? NAMA_HARI[idx] : "-";
    }

    private static String namaPenanggungJawab(Kelas kelas) {
        return kelas.getPenanggungJawab() != null ? kelas.getPenanggungJawab().getNama() : null;
    }
}
