package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.mahasiswa.JadwalPresentasiTerdekatDTO;
import com.unpar.scheduler.dto.mahasiswa.MahasiswaContextDTO;
import com.unpar.scheduler.dto.mahasiswa.MahasiswaDashboardDTO;
import com.unpar.scheduler.dto.mahasiswa.MatkulDiambilDTO;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Mahasiswa;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.PesertaKelas;
import com.unpar.scheduler.model.entity.Presentasi;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import com.unpar.scheduler.repository.PresentasiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Merangkum status input mata kuliah dan jadwal presentasi terdekat milik mahasiswa pada semester aktif.
@Service
@Transactional(readOnly = true)
public class MahasiswaDashboardService {

    static final String STATUS_AKAN_DATANG = "Akan Datang";
    static final String STATUS_SELESAI = "Selesai";

    private static final Comparator<Presentasi> BERDASARKAN_WAKTU_MULAI = Comparator
        .comparing((Presentasi presentasi) -> LocalDateTime.of(presentasi.getTanggal(), presentasi.getJamMulai()))
        .thenComparing(Presentasi::getIdPresentasi, Comparator.nullsLast(Comparator.naturalOrder()));

    private final SemesterAkademikResolver semesterAkademikResolver;
    private final PesertaKelasRepository pesertaKelasRepository;
    private final PresentasiRepository presentasiRepository;
    private final NamaDosenPengajarResolver namaDosenPengajarResolver;

    public MahasiswaDashboardService(
        SemesterAkademikResolver semesterAkademikResolver,
        PesertaKelasRepository pesertaKelasRepository,
        PresentasiRepository presentasiRepository,
        NamaDosenPengajarResolver namaDosenPengajarResolver
    ) {
        this.semesterAkademikResolver = semesterAkademikResolver;
        this.pesertaKelasRepository = pesertaKelasRepository;
        this.presentasiRepository = presentasiRepository;
        this.namaDosenPengajarResolver = namaDosenPengajarResolver;
    }

    public MahasiswaContextDTO getContext(Mahasiswa mahasiswa) {
        String npmBersih = mahasiswa.getNpm().trim();
        String labelSemester = SemesterAkademikResolver.formatSemesterLabel(
            semesterAkademikResolver.resolveActiveSemesterForMahasiswa(npmBersih).orElse(null)
        );
        return new MahasiswaContextDTO(mahasiswa.getNama(), npmBersih, labelSemester);
    }

    public MahasiswaDashboardDTO getDashboard(Mahasiswa mahasiswa, LocalDateTime sekarang) {
        String npmBersih = mahasiswa.getNpm().trim();
        Optional<String> semester = semesterAkademikResolver.resolveActiveSemesterForMahasiswa(npmBersih);
        String labelSemester = SemesterAkademikResolver.formatSemesterLabel(semester.orElse(null));
        if (semester.isEmpty()) {
            return new MahasiswaDashboardDTO(mahasiswa.getNama(), npmBersih, labelSemester, false, List.of(), null);
        }

        List<PesertaKelas> peserta = pesertaKelasRepository.findByNpmAndSemesterWithDetails(mahasiswa.getNpm(), semester.get());
        if (peserta.isEmpty()) {
            return new MahasiswaDashboardDTO(mahasiswa.getNama(), npmBersih, labelSemester, false, List.of(), null);
        }

        List<Kelas> daftarKelas = peserta.stream().map(PesertaKelas::getKelas).toList();
        Map<Integer, String> dosenPerKelas = namaDosenPengajarResolver.resolve(daftarKelas);
        List<MatkulDiambilDTO> matkulDiambil = daftarKelas.stream()
            .sorted(Comparator.comparing(kelas -> kelas.getMataKuliahBuka().getMatakuliah().getIdMatkul()))
            .map(kelas -> {
                MataKuliah matkul = kelas.getMataKuliahBuka().getMatakuliah();
                return new MatkulDiambilDTO(
                    kelas.getIdKelas(),
                    matkul.getIdMatkul(),
                    matkul.getNama(),
                    kelas.getKodeKelas(),
                    dosenPerKelas.getOrDefault(kelas.getIdKelas(), NamaDosenPengajarResolver.TANPA_DOSEN)
                );
            })
            .toList();

        JadwalPresentasiTerdekatDTO jadwal = pilihJadwalTerdekat(
            presentasiRepository.findByMahasiswaNpmAndSemester(mahasiswa.getNpm(), semester.get()), sekarang
        );
        return new MahasiswaDashboardDTO(mahasiswa.getNama(), npmBersih, labelSemester, true, matkulDiambil, jadwal);
    }

    // Presentasi yang belum berakhir dan paling awal diutamakan; bila semuanya sudah lewat, tampilkan yang terakhir.
    private JadwalPresentasiTerdekatDTO pilihJadwalTerdekat(List<Presentasi> daftarPresentasi, LocalDateTime sekarang) {
        Optional<Presentasi> akanDatang = daftarPresentasi.stream()
            .filter(presentasi -> !LocalDateTime.of(presentasi.getTanggal(), presentasi.getJamSelesai()).isBefore(sekarang))
            .min(BERDASARKAN_WAKTU_MULAI);
        if (akanDatang.isPresent()) {
            return toDto(akanDatang.get(), STATUS_AKAN_DATANG);
        }
        return daftarPresentasi.stream().max(BERDASARKAN_WAKTU_MULAI)
            .map(presentasi -> toDto(presentasi, STATUS_SELESAI))
            .orElse(null);
    }

    private JadwalPresentasiTerdekatDTO toDto(Presentasi presentasi, String status) {
        MataKuliah matkul = presentasi.getKelompok().getKelas().getMataKuliahBuka().getMatakuliah();
        return new JadwalPresentasiTerdekatDTO(
            matkul.getIdMatkul(),
            matkul.getNama(),
            presentasi.getTanggal(),
            presentasi.getJamMulai(),
            presentasi.getJamSelesai(),
            presentasi.getRuangan(),
            presentasi.getKelompok().getNoKelompok(),
            status
        );
    }
}
