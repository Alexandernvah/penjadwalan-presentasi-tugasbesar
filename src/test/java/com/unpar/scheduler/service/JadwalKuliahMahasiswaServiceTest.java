package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.jadwalkuliah.JadwalKuliahActionResponseDTO;
import com.unpar.scheduler.dto.jadwalkuliah.JadwalKuliahSayaItemDTO;
import com.unpar.scheduler.dto.jadwalkuliah.KatalogJadwalKuliahResponseDTO;
import com.unpar.scheduler.dto.jadwalkuliah.MataKuliahPilihanDTO;
import com.unpar.scheduler.dto.jadwalkuliah.PilihKelasRequestDTO;
import com.unpar.scheduler.model.entity.Akun;
import com.unpar.scheduler.model.entity.Dosen;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kuliah;
import com.unpar.scheduler.model.entity.Mahasiswa;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.MataKuliahBuka;
import com.unpar.scheduler.model.entity.PesertaKelas;
import com.unpar.scheduler.repository.AnggotaKelompokRepository;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KuliahRepository;
import com.unpar.scheduler.repository.MahasiswaRepository;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JadwalKuliahMahasiswaServiceTest {

    private static final String NPM = "6182301001";
    private static final String SEMESTER = "25261";

    @Mock
    private MahasiswaRepository mahasiswaRepository;
    @Mock
    private KelasRepository kelasRepository;
    @Mock
    private KuliahRepository kuliahRepository;
    @Mock
    private PesertaKelasRepository pesertaKelasRepository;
    @Mock
    private AnggotaKelompokRepository anggotaKelompokRepository;

    private JadwalKuliahMahasiswaService service;

    private Dosen budi;
    private Mahasiswa rina;
    private MataKuliahBuka bukaAsd;
    private MataKuliahBuka bukaJarkom;
    private Kelas asdA;
    private Kelas asdB;
    private Kelas jarkomB;

    @BeforeEach
    void setUp() {
        service = new JadwalKuliahMahasiswaService(
            mahasiswaRepository, kelasRepository, kuliahRepository, pesertaKelasRepository, anggotaKelompokRepository
        );
        budi = new Dosen("1975031501", "Dr. Budi Santoso", new Akun());
        rina = new Mahasiswa(NPM, "Rina Maharani", new Akun());
        bukaAsd = new MataKuliahBuka(1, new MataKuliah("IF2110", "Algoritma dan Struktur Data", (short) 4), SEMESTER, budi);
        bukaJarkom = new MataKuliahBuka(2, new MataKuliah("IF3130", "Jaringan Komputer", (short) 3), SEMESTER, budi);
        asdA = new Kelas(101, bukaAsd, "A", budi);
        asdB = new Kelas(104, bukaAsd, "B", null);
        jarkomB = new Kelas(102, bukaJarkom, "B", budi);
    }

    private Kuliah kuliah(int id, Kelas kelas, int hari, int jam, String ruangan) {
        return new Kuliah(id, kelas, budi, (short) hari, LocalTime.of(jam, 0), LocalTime.of(jam + 2, 30), ruangan, "KULIAH");
    }

    @Test
    void katalogMenampilkanMataKuliahDenganKelasJadwalDanStatusTerdaftar() {
        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.of(SEMESTER));
        when(kelasRepository.findBySemesterWithDetails(SEMESTER)).thenReturn(List.of(asdA, asdB, jarkomB));
        when(kuliahRepository.findByKelasIdsWithDetails(anyCollection())).thenReturn(List.of(
            kuliah(2, asdA, 3, 10, "Ruang 9010"),
            kuliah(1, asdA, 1, 8, "Lab Komputer 1"),
            kuliah(3, jarkomB, 3, 13, "Ruang 9122")
        ));
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER))
            .thenReturn(List.of(new PesertaKelas(1, rina, asdA)));

        KatalogJadwalKuliahResponseDTO katalog = service.getKatalog(NPM);

        assertEquals(SEMESTER, katalog.semesterAkademik());
        assertEquals(2, katalog.mataKuliah().size());

        MataKuliahPilihanDTO asd = katalog.mataKuliah().get(0);
        assertEquals("IF2110", asd.idMatkul());
        assertEquals(Integer.valueOf(101), asd.idKelasTerdaftar());
        assertEquals(2, asd.kelas().size());
        assertTrue(asd.kelas().get(0).terdaftar());
        assertFalse(asd.kelas().get(1).terdaftar());
        assertNull(asd.kelas().get(1).dosenPenanggungJawab());
        // slot diurutkan menurut hari, lalu jam
        assertEquals("Senin", asd.kelas().get(0).jadwal().get(0).namaHari());
        assertEquals("08:00", asd.kelas().get(0).jadwal().get(0).jamMulai());
        assertEquals("10:30", asd.kelas().get(0).jadwal().get(0).jamSelesai());
        assertEquals("Lab Komputer 1", asd.kelas().get(0).jadwal().get(0).ruangan());
        assertEquals("Rabu", asd.kelas().get(0).jadwal().get(1).namaHari());
        assertTrue(asd.kelas().get(1).jadwal().isEmpty());

        MataKuliahPilihanDTO jarkom = katalog.mataKuliah().get(1);
        assertNull(jarkom.idKelasTerdaftar());
        assertFalse(jarkom.kelas().get(0).terdaftar());
    }

    @Test
    void katalogKosongBilaBelumAdaSemester() {
        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.empty());

        KatalogJadwalKuliahResponseDTO katalog = service.getKatalog(NPM);

        assertNull(katalog.semesterAkademik());
        assertTrue(katalog.mataKuliah().isEmpty());
    }

    @Test
    void jadwalSayaMemuatKelasTerdaftarBerurutanHari() {
        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.of(SEMESTER));
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER)).thenReturn(List.of(
            new PesertaKelas(2, rina, jarkomB),
            new PesertaKelas(1, rina, asdA)
        ));
        when(kuliahRepository.findByKelasIdsWithDetails(anyCollection())).thenReturn(List.of(
            kuliah(1, asdA, 1, 8, "Lab Komputer 1"),
            kuliah(3, jarkomB, 3, 13, "Ruang 9122")
        ));

        List<JadwalKuliahSayaItemDTO> jadwal = service.getJadwalSaya(NPM);

        assertEquals(2, jadwal.size());
        assertEquals("Algoritma dan Struktur Data", jadwal.get(0).namaMatkul());
        assertEquals("A", jadwal.get(0).kodeKelas());
        assertEquals("Jaringan Komputer", jadwal.get(1).namaMatkul());
    }

    @Test
    void tambahKelasBerhasilMenyimpanPeserta() {
        when(mahasiswaRepository.findByNpm(NPM)).thenReturn(Optional.of(rina));
        when(kelasRepository.findByIdWithDetails(101)).thenReturn(Optional.of(asdA));
        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.of(SEMESTER));
        when(pesertaKelasRepository.findByMahasiswa_NpmAndKelas_IdKelas(NPM, 101)).thenReturn(Optional.empty());
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER)).thenReturn(Collections.emptyList());

        JadwalKuliahActionResponseDTO response = service.tambahKelas(NPM, new PilihKelasRequestDTO(101));

        assertTrue(response.success());
        verify(pesertaKelasRepository).save(any(PesertaKelas.class));
    }

    @Test
    void tambahKelasDitolakBilaMataKuliahSamaSudahDiambilDiKelasLain() {
        when(mahasiswaRepository.findByNpm(NPM)).thenReturn(Optional.of(rina));
        when(kelasRepository.findByIdWithDetails(104)).thenReturn(Optional.of(asdB));
        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.of(SEMESTER));
        when(pesertaKelasRepository.findByMahasiswa_NpmAndKelas_IdKelas(NPM, 104)).thenReturn(Optional.empty());
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER))
            .thenReturn(List.of(new PesertaKelas(1, rina, asdA)));

        JadwalKuliahActionResponseDTO response = service.tambahKelas(NPM, new PilihKelasRequestDTO(104));

        assertFalse(response.success());
        assertTrue(response.message().contains("Kelas A"));
        verify(pesertaKelasRepository, never()).save(any(PesertaKelas.class));
    }

    @Test
    void tambahKelasDitolakBilaSudahTerdaftarDiKelasYangSama() {
        when(mahasiswaRepository.findByNpm(NPM)).thenReturn(Optional.of(rina));
        when(kelasRepository.findByIdWithDetails(101)).thenReturn(Optional.of(asdA));
        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.of(SEMESTER));
        when(pesertaKelasRepository.findByMahasiswa_NpmAndKelas_IdKelas(NPM, 101))
            .thenReturn(Optional.of(new PesertaKelas(1, rina, asdA)));

        JadwalKuliahActionResponseDTO response = service.tambahKelas(NPM, new PilihKelasRequestDTO(101));

        assertFalse(response.success());
        verify(pesertaKelasRepository, never()).save(any(PesertaKelas.class));
    }

    @Test
    void tambahKelasMenolakKelasDiLuarSemesterBerjalan() {
        MataKuliahBuka lama = new MataKuliahBuka(9, bukaAsd.getMatakuliah(), "24262", budi);
        Kelas kelasLama = new Kelas(90, lama, "A", budi);
        when(mahasiswaRepository.findByNpm(NPM)).thenReturn(Optional.of(rina));
        when(kelasRepository.findByIdWithDetails(90)).thenReturn(Optional.of(kelasLama));
        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.of(SEMESTER));

        assertThrows(IllegalArgumentException.class, () -> service.tambahKelas(NPM, new PilihKelasRequestDTO(90)));
        verify(pesertaKelasRepository, never()).save(any(PesertaKelas.class));
    }

    @Test
    void tambahKelasTanpaPilihanKelasDitolak() {
        assertThrows(IllegalArgumentException.class, () -> service.tambahKelas(NPM, new PilihKelasRequestDTO(null)));
        assertThrows(IllegalArgumentException.class, () -> service.tambahKelas(NPM, null));
    }

    @Test
    void hapusKelasBerhasilMenghapusPeserta() {
        PesertaKelas peserta = new PesertaKelas(1, rina, asdA);
        when(pesertaKelasRepository.findByMahasiswa_NpmAndKelas_IdKelas(NPM, 101)).thenReturn(Optional.of(peserta));
        when(anggotaKelompokRepository.existsByMahasiswa_NpmAndKelas_IdKelas(NPM, 101)).thenReturn(false);

        JadwalKuliahActionResponseDTO response = service.hapusKelas(NPM, 101);

        assertTrue(response.success());
        verify(pesertaKelasRepository).delete(peserta);
    }

    @Test
    void hapusKelasDitolakBilaSudahMenjadiAnggotaKelompok() {
        when(pesertaKelasRepository.findByMahasiswa_NpmAndKelas_IdKelas(NPM, 101))
            .thenReturn(Optional.of(new PesertaKelas(1, rina, asdA)));
        when(anggotaKelompokRepository.existsByMahasiswa_NpmAndKelas_IdKelas(NPM, 101)).thenReturn(true);

        JadwalKuliahActionResponseDTO response = service.hapusKelas(NPM, 101);

        assertFalse(response.success());
        assertTrue(response.message().contains("kelompok"));
        verify(pesertaKelasRepository, never()).delete(any(PesertaKelas.class));
    }

    @Test
    void hapusKelasYangTidakAdaDiJadwalDitolak() {
        when(pesertaKelasRepository.findByMahasiswa_NpmAndKelas_IdKelas(NPM, 999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.hapusKelas(NPM, 999));
    }
}
