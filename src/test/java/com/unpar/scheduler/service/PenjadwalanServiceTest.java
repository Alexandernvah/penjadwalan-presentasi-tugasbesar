package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.penjadwalan.BarisMatriksMingguanDTO;
import com.unpar.scheduler.dto.penjadwalan.CekBentrokRequestDTO;
import com.unpar.scheduler.dto.penjadwalan.CekBentrokResponseDTO;
import com.unpar.scheduler.dto.penjadwalan.KelompokPenjadwalanDTO;
import com.unpar.scheduler.dto.penjadwalan.MatriksMingguanResponseDTO;
import com.unpar.scheduler.dto.penjadwalan.MatriksPenjadwalanResponseDTO;
import com.unpar.scheduler.dto.penjadwalan.PresentasiTerjadwalDTO;
import com.unpar.scheduler.dto.penjadwalan.SelMatriksMingguanDTO;
import com.unpar.scheduler.dto.penjadwalan.SimpanPresentasiRequestDTO;
import com.unpar.scheduler.model.entity.Akun;
import com.unpar.scheduler.model.entity.AnggotaKelompok;
import com.unpar.scheduler.model.entity.Dosen;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kelompok;
import com.unpar.scheduler.model.entity.Kuliah;
import com.unpar.scheduler.model.entity.Mahasiswa;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.MataKuliahBuka;
import com.unpar.scheduler.model.entity.PesertaKelas;
import com.unpar.scheduler.model.entity.Presentasi;
import com.unpar.scheduler.repository.AnggotaKelompokRepository;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KelompokRepository;
import com.unpar.scheduler.repository.KuliahRepository;
import com.unpar.scheduler.repository.PengawasUjianRepository;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import com.unpar.scheduler.repository.PresentasiRepository;
import com.unpar.scheduler.repository.UjianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PenjadwalanServiceTest {

    @Mock
    private DosenDashboardService dosenDashboardService;
    @Mock
    private KelasRepository kelasRepository;
    @Mock
    private KelompokRepository kelompokRepository;
    @Mock
    private AnggotaKelompokRepository anggotaKelompokRepository;
    @Mock
    private PesertaKelasRepository pesertaKelasRepository;
    @Mock
    private KuliahRepository kuliahRepository;
    @Mock
    private UjianRepository ujianRepository;
    @Mock
    private PengawasUjianRepository pengawasUjianRepository;
    @Mock
    private PresentasiRepository presentasiRepository;

    private PenjadwalanService penjadwalanService;

    private Dosen dosenBudi;
    private MataKuliah mkJarkom;
    private MataKuliah mkAsd;
    private MataKuliahBuka mkbJarkom;
    private MataKuliahBuka mkbAsd;
    private Kelas kelasJarkom;
    private Kelas kelasAsd;
    private Kelompok kelompok1;
    private Mahasiswa mhsAndi;
    private Mahasiswa mhsNurul;

    @BeforeEach
    void setUp() {
        penjadwalanService = new PenjadwalanService(
            dosenDashboardService,
            kelasRepository,
            kelompokRepository,
            anggotaKelompokRepository,
            pesertaKelasRepository,
            kuliahRepository,
            ujianRepository,
            pengawasUjianRepository,
            presentasiRepository
        );

        Akun akunDosen = new Akun();
        dosenBudi = new Dosen("1975031501", "Dr. Budi Santoso", akunDosen);

        mkJarkom = new MataKuliah("IF3130", "Jaringan Komputer", (short) 3);
        mkAsd = new MataKuliah("IF2110", "Algoritma dan Struktur Data", (short) 4);

        mkbJarkom = new MataKuliahBuka(10, mkJarkom, "25261", dosenBudi);
        mkbAsd = new MataKuliahBuka(11, mkAsd, "25261", dosenBudi);

        kelasJarkom = new Kelas(102, mkbJarkom, "B", dosenBudi);
        kelasAsd = new Kelas(105, mkbAsd, "A", dosenBudi);

        kelompok1 = new Kelompok(1, kelasJarkom, (short) 1, (short) 4, "Perancangan Topologi Jaringan Kampus");
        mhsAndi = new Mahasiswa("6182101001", "Andi Saputra", new Akun());
        mhsNurul = new Mahasiswa("6182101002", "Nurul Hidayah", new Akun());
    }

    @Test
    void testOverlapPredicate() {
        assertTrue(PenjadwalanService.isOverlapping(
            LocalTime.of(8, 0), LocalTime.of(9, 0),
            LocalTime.of(8, 30), LocalTime.of(9, 30)
        ));
        assertFalse(PenjadwalanService.isOverlapping(
            LocalTime.of(8, 0), LocalTime.of(9, 0),
            LocalTime.of(9, 0), LocalTime.of(10, 0)
        ));
    }

    @Test
    void testKelompokLeaderFlagAndMetadata() {
        AnggotaKelompok ak1 = new AnggotaKelompok(1, mhsAndi, kelompok1, kelasJarkom);
        AnggotaKelompok ak2 = new AnggotaKelompok(2, mhsNurul, kelompok1, kelasJarkom);

        when(kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(102)).thenReturn(List.of(kelompok1));
        when(anggotaKelompokRepository.findByKelasIdWithMahasiswaAndKelompok(102)).thenReturn(List.of(ak1, ak2));
        when(presentasiRepository.findByKelasIdWithDetails(102)).thenReturn(Collections.emptyList());

        List<KelompokPenjadwalanDTO> groups = penjadwalanService.getKelompokByKelas(102);
        assertEquals(1, groups.size());
        KelompokPenjadwalanDTO g = groups.get(0);
        assertEquals(1, g.noKelompok());
        assertEquals(2, g.anggota().size());
        assertTrue(g.anggota().get(0).isKetua());
        assertFalse(g.anggota().get(1).isKetua());
    }

    @Test
    void testWeeklyMatrixStatesAndRowSpan() {
        LocalDate monday = LocalDate.of(2026, 10, 5);
        LocalDate friday = LocalDate.of(2026, 10, 9);

        AnggotaKelompok akAndi = new AnggotaKelompok(1, mhsAndi, kelompok1, kelasJarkom);
        PesertaKelas pkAndiAsd = new PesertaKelas(1, mhsAndi, kelasAsd);

        // Monday 08:00 - 10:00: Student Andi has Kuliah ASD (BENTROK_MAHASISWA)
        Dosen dosenLain = new Dosen("1980010101", "Dr. Hendra", new Akun());
        Kuliah kuliahAsdMhs = new Kuliah(
            50, kelasAsd, dosenLain, (short) 1,
            LocalTime.of(8, 0), LocalTime.of(10, 0), "Ruang 9121", "KULIAH"
        );

        // Tuesday 10:00 - 12:00: Lecturer Budi teaches another class (BENTROK_DOSEN)
        Kuliah kuliahDosenLain = new Kuliah(
            51, kelasAsd, dosenBudi, (short) 2,
            LocalTime.of(10, 0), LocalTime.of(12, 0), "Lab Komputer 1", "KULIAH"
        );

        // Wednesday 13:00 - 15:00: Jarkom Regular Class (PRIORITAS_KELAS)
        Kuliah kuliahRegulerJarkom = new Kuliah(
            52, kelasJarkom, dosenBudi, (short) 3,
            LocalTime.of(13, 0), LocalTime.of(15, 0), "Ruang 9122", "KULIAH"
        );

        when(kelasRepository.findByIdWithDetails(102)).thenReturn(Optional.of(kelasJarkom));
        when(kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(102)).thenReturn(List.of(kelompok1));
        when(kuliahRepository.findByKelas_IdKelas(102)).thenReturn(List.of(kuliahRegulerJarkom));
        when(kuliahRepository.findByDosenIdsWithDetails(anyCollection()))
            .thenReturn(List.of(kuliahDosenLain, kuliahRegulerJarkom));
        when(pengawasUjianRepository.findByTanggalBetweenAndDosenIdsWithDetails(eq(monday), eq(friday), anyCollection()))
            .thenReturn(Collections.emptyList());
        when(presentasiRepository.findByTanggalBetweenWithDetails(monday, friday))
            .thenReturn(Collections.emptyList());
        when(anggotaKelompokRepository.findByKelasIdWithMahasiswaAndKelompok(102)).thenReturn(List.of(akAndi));
        when(pesertaKelasRepository.findByMahasiswaNpmInWithDetails(anyCollection())).thenReturn(List.of(pkAndiAsd));
        when(kuliahRepository.findByKelasIdsWithDetails(anyCollection())).thenReturn(List.of(kuliahAsdMhs));
        when(ujianRepository.findByTanggalBetweenAndMatkulBukaIds(eq(monday), eq(friday), anyCollection()))
            .thenReturn(Collections.emptyList());
        when(anggotaKelompokRepository.findByMahasiswaNpmInWithDetails(anyCollection())).thenReturn(List.of(akAndi));
        when(presentasiRepository.findByKelasIdWithDetails(102)).thenReturn(Collections.emptyList());

        MatriksMingguanResponseDTO matrix = penjadwalanService.getMatriksMingguan(102, 1, "REGULER", monday, "1975031501");

        assertNotNull(matrix);
        assertEquals(5, matrix.hariList().size());
        assertEquals(11, matrix.barisList().size());
        assertEquals("05 Oktober 2026 - 09 Oktober 2026", matrix.rentangTanggalLabel());

        // Row 08:00 - 09:00 (index 1), Monday (index 0) -> BENTROK_MAHASISWA with rowSpan=2
        BarisMatriksMingguanDTO row08 = matrix.barisList().get(1);
        SelMatriksMingguanDTO mon08 = row08.selList().get(0);
        assertEquals("BENTROK_MAHASISWA", mon08.status());
        assertEquals(2, mon08.rowSpan());
        assertFalse(mon08.skipRender());
        assertEquals("08:00 - 10:00", mon08.labelRentangJam());
        assertTrue(mon08.deskripsi().contains("Andi"));

        // Row 09:00 - 10:00 (index 2), Monday (index 0) -> skipRender=true
        SelMatriksMingguanDTO mon09 = matrix.barisList().get(2).selList().get(0);
        assertTrue(mon09.skipRender());

        // Row 10:00 - 11:00 (index 3), Tuesday (index 1) -> BENTROK_DOSEN with rowSpan=2
        SelMatriksMingguanDTO tue10 = matrix.barisList().get(3).selList().get(1);
        assertEquals("BENTROK_DOSEN", tue10.status());
        assertEquals(2, tue10.rowSpan());

        // Row 13:00 - 14:00 (index 6), Wednesday (index 2) -> PRIORITAS_KELAS with rowSpan=2
        SelMatriksMingguanDTO wed13 = matrix.barisList().get(6).selList().get(2);
        assertEquals("PRIORITAS_KELAS", wed13.status());
        assertEquals(2, wed13.rowSpan());
        assertEquals("Aman, Semua Bisa Hadir", wed13.deskripsi());
    }

    @Test
    void testSimpanPresentasi_ForceOverrideBehavior() {
        LocalDate monday = LocalDate.of(2026, 10, 5);
        AnggotaKelompok akAndi = new AnggotaKelompok(1, mhsAndi, kelompok1, kelasJarkom);
        PesertaKelas pkAndiAsd = new PesertaKelas(1, mhsAndi, kelasAsd);
        Dosen dosenLain = new Dosen("1980010101", "Dr. Hendra", new Akun());
        Kuliah kuliahAsdMhs = new Kuliah(
            50, kelasAsd, dosenLain, (short) 1,
            LocalTime.of(8, 0), LocalTime.of(10, 0), "Ruang 9121", "KULIAH"
        );

        when(kelasRepository.findByIdWithDetails(102)).thenReturn(Optional.of(kelasJarkom));
        when(kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(102)).thenReturn(List.of(kelompok1));
        when(kelompokRepository.findById(1)).thenReturn(Optional.of(kelompok1));
        when(kuliahRepository.findByKelas_IdKelas(102)).thenReturn(Collections.emptyList());
        when(kuliahRepository.findByHariAndDosenIdsWithDetails(eq((short) 1), anyCollection()))
            .thenReturn(Collections.emptyList());
        when(pengawasUjianRepository.findByTanggalAndDosenIdsWithDetails(eq(monday), anyCollection()))
            .thenReturn(Collections.emptyList());
        when(presentasiRepository.findByTanggalWithDetails(monday)).thenReturn(Collections.emptyList());

        when(anggotaKelompokRepository.findByKelasIdWithMahasiswaAndKelompok(102)).thenReturn(List.of(akAndi));
        when(pesertaKelasRepository.findByMahasiswaNpmInWithDetails(anyCollection())).thenReturn(List.of(pkAndiAsd));
        when(kuliahRepository.findByHariAndKelasIdsWithDetails(eq((short) 1), anyCollection()))
            .thenReturn(List.of(kuliahAsdMhs));
        when(ujianRepository.findByTanggalAndMatkulBukaIds(eq(monday), anyCollection()))
            .thenReturn(Collections.emptyList());
        when(anggotaKelompokRepository.findByMahasiswaNpmInWithDetails(anyCollection())).thenReturn(List.of(akAndi));
        when(presentasiRepository.findByTanggalAndKelompokIdsWithDetails(eq(monday), anyCollection()))
            .thenReturn(Collections.emptyList());

        when(kuliahRepository.findByHariAndRuanganWithDetails((short) 1, "Ruang 9122")).thenReturn(Collections.emptyList());
        when(ujianRepository.findByTanggalAndRuanganWithDetails(monday, "Ruang 9122")).thenReturn(Collections.emptyList());
        when(presentasiRepository.findByTanggalAndRuanganWithDetails(monday, "Ruang 9122")).thenReturn(Collections.emptyList());
        when(presentasiRepository.findByKelasIdWithDetails(102)).thenReturn(Collections.emptyList());
        when(anggotaKelompokRepository.findByKelompok_IdKelompok(1)).thenReturn(List.of(akAndi));

        // 1. Without forceOverride -> throws ConflictScheduleException (HTTP 409)
        assertThrows(ConflictScheduleException.class, () ->
            penjadwalanService.simpanPresentasi(new SimpanPresentasiRequestDTO(
                102, 1, "Ruang 9122", "Ruang 9122", monday.toString(), "08:00", "08:30", 30, null, false, "1975031501"
            ))
        );

        // 2. With forceOverride = true -> succeeds over partial student conflict
        when(presentasiRepository.save(any(Presentasi.class))).thenAnswer(inv -> {
            Presentasi p = inv.getArgument(0);
            p.setIdPresentasi(88);
            assertTrue(p.getAdaBentrok());
            return p;
        });

        PresentasiTerjadwalDTO saved = penjadwalanService.simpanPresentasi(new SimpanPresentasiRequestDTO(
            102, 1, "Ruang 9122", "Ruang 9122", monday.toString(), "08:00", "08:30", 30, null, true, "1975031501"
        ));

        assertNotNull(saved);
        assertEquals(88, saved.idPresentasi());
        assertEquals("08:00", saved.jamMulai());
        assertEquals("08:30", saved.jamSelesai());
    }

    @Test
    void testSimpanPresentasiWhenSlotAvailable() {
        LocalDate monday = LocalDate.of(2026, 10, 5);

        when(kelasRepository.findByIdWithDetails(102)).thenReturn(Optional.of(kelasJarkom));
        when(kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(102)).thenReturn(List.of(kelompok1));
        when(kelompokRepository.findById(1)).thenReturn(Optional.of(kelompok1));
        when(kuliahRepository.findByKelas_IdKelas(102)).thenReturn(Collections.emptyList());
        when(kuliahRepository.findByHariAndDosenIdsWithDetails(eq((short) 1), anyCollection())).thenReturn(Collections.emptyList());
        when(pengawasUjianRepository.findByTanggalAndDosenIdsWithDetails(eq(monday), anyCollection())).thenReturn(Collections.emptyList());
        when(presentasiRepository.findByTanggalWithDetails(monday)).thenReturn(Collections.emptyList());
        when(anggotaKelompokRepository.findByKelasIdWithMahasiswaAndKelompok(102)).thenReturn(Collections.emptyList());
        when(kuliahRepository.findByHariAndRuanganWithDetails((short) 1, "Ruang 9122")).thenReturn(Collections.emptyList());
        when(ujianRepository.findByTanggalAndRuanganWithDetails(monday, "Ruang 9122")).thenReturn(Collections.emptyList());
        when(presentasiRepository.findByTanggalAndRuanganWithDetails(monday, "Ruang 9122")).thenReturn(Collections.emptyList());
        when(presentasiRepository.findByKelasIdWithDetails(102)).thenReturn(Collections.emptyList());
        when(anggotaKelompokRepository.findByKelompok_IdKelompok(1)).thenReturn(Collections.emptyList());

        when(presentasiRepository.save(any(Presentasi.class))).thenAnswer(inv -> {
            Presentasi p = inv.getArgument(0);
            p.setIdPresentasi(77);
            return p;
        });

        CekBentrokResponseDTO check = penjadwalanService.cekBentrok(new CekBentrokRequestDTO(
            102, 1, "Ruang 9122", monday.toString(), "10:00", 30, "1975031501"
        ));
        assertFalse(check.adaBentrok());

        PresentasiTerjadwalDTO saved = penjadwalanService.simpanPresentasi(new SimpanPresentasiRequestDTO(
            102, 1, "Ruang 9122", "Ruang 9122", monday.toString(), "10:00", "10:30", 30, "Presentasi Akhir", false, "1975031501"
        ));

        assertNotNull(saved);
        assertEquals(77, saved.idPresentasi());
        assertEquals("10:00", saved.jamMulai());
        assertEquals("10:30", saved.jamSelesai());

        MatriksPenjadwalanResponseDTO matriks = penjadwalanService.getMatriksHarian(102, "Ruang 9122", monday, null, "1975031501");
        assertEquals(10, matriks.slots().size());
    }

    @Test
    void testWeeklyMatrix_BlocksAlreadyScheduledPresentationSlots() {
        LocalDate monday = LocalDate.of(2026, 10, 5);
        LocalDate wednesday = LocalDate.of(2026, 10, 7);
        LocalDate friday = LocalDate.of(2026, 10, 9);

        Kelompok kelompok2 = new Kelompok(2, kelasJarkom, (short) 2, (short) 4, "Sistem Keamanan Jaringan");

        Presentasi presKel1 = new Presentasi(
            99, kelompok1, wednesday, LocalTime.of(9, 0), LocalTime.of(9, 30), "Lab Komputer 1", false, null
        );

        when(kelasRepository.findByIdWithDetails(102)).thenReturn(Optional.of(kelasJarkom));
        when(kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(102)).thenReturn(List.of(kelompok1, kelompok2));
        when(kuliahRepository.findByKelas_IdKelas(102)).thenReturn(Collections.emptyList());
        when(kuliahRepository.findByDosenIdsWithDetails(anyCollection())).thenReturn(Collections.emptyList());
        when(pengawasUjianRepository.findByTanggalBetweenAndDosenIdsWithDetails(eq(monday), eq(friday), anyCollection()))
            .thenReturn(Collections.emptyList());
        when(presentasiRepository.findByTanggalBetweenWithDetails(monday, friday))
            .thenReturn(List.of(presKel1));
        when(anggotaKelompokRepository.findByKelasIdWithMahasiswaAndKelompok(102)).thenReturn(Collections.emptyList());
        when(ujianRepository.findByTanggalBetweenAndMatkulBukaIds(eq(monday), eq(friday), anyCollection()))
            .thenReturn(Collections.emptyList());
        when(presentasiRepository.findByKelasIdWithDetails(102)).thenReturn(List.of(presKel1));

        // 1. Matrix for Kelompok 1 (the scheduled group) -> slot 09:00 - 10:00 on Wednesday MUST be TERJADWAL
        MatriksMingguanResponseDTO matrixKel1 = penjadwalanService.getMatriksMingguan(102, 1, "REGULER", monday, "1975031501");
        assertNotNull(matrixKel1);
        BarisMatriksMingguanDTO row09 = matrixKel1.barisList().get(2); // 09:00 - 10:00 is index 2
        assertEquals("09:00 - 10:00", row09.labelWaktu());
        SelMatriksMingguanDTO wed09Kel1 = row09.selList().get(2); // Wednesday is index 2
        assertEquals("TERJADWAL", wed09Kel1.status());
        assertEquals("Terjadwal", wed09Kel1.badgeText());
        assertEquals("Kelompok 1\nJaringan Komputer\n09.00 - 09.30", wed09Kel1.deskripsi());
        assertEquals(Integer.valueOf(99), wed09Kel1.idPresentasiTerjadwal());
        assertTrue(wed09Kel1.intervalTerpakai().contains("09:00 - 09:30"));

        // 2. Matrix for Kelompok 2 (different group) -> slot 09:00 - 10:00 on Wednesday MUST be BENTROK_DOSEN with course name
        MatriksMingguanResponseDTO matrixKel2 = penjadwalanService.getMatriksMingguan(102, 2, "REGULER", monday, "1975031501");
        assertNotNull(matrixKel2);
        SelMatriksMingguanDTO wed09Kel2 = matrixKel2.barisList().get(2).selList().get(2);
        assertEquals("BENTROK_DOSEN", wed09Kel2.status());
        assertEquals("Dosen Sibuk", wed09Kel2.badgeText());
        assertEquals("Menguji Kelompok 1\nJaringan Komputer\n09.00 - 09.30", wed09Kel2.deskripsi());
        assertTrue(wed09Kel2.intervalTerpakai().contains("09:00 - 09:30"));

        // 3. Cross-course check: Kelompok 1 in ASD (idKelas=105) sees Kelompok 1 in Jaringan Komputer as BENTROK_DOSEN
        Kelompok kelompok1Asd = new Kelompok(3, kelasAsd, (short) 1, (short) 4, "Struktur Data");
        when(kelasRepository.findByIdWithDetails(105)).thenReturn(Optional.of(kelasAsd));
        when(kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(105)).thenReturn(List.of(kelompok1Asd));
        when(kuliahRepository.findByKelas_IdKelas(105)).thenReturn(Collections.emptyList());
        when(anggotaKelompokRepository.findByKelasIdWithMahasiswaAndKelompok(105)).thenReturn(Collections.emptyList());
        when(presentasiRepository.findByKelasIdWithDetails(105)).thenReturn(Collections.emptyList());

        MatriksMingguanResponseDTO matrixAsd = penjadwalanService.getMatriksMingguan(105, 3, "REGULER", monday, "1975031501");
        SelMatriksMingguanDTO wed09Asd = matrixAsd.barisList().get(2).selList().get(2);
        assertEquals("BENTROK_DOSEN", wed09Asd.status());
        assertEquals("Dosen Sibuk", wed09Asd.badgeText());
        assertEquals("Menguji Kelompok 1\nJaringan Komputer\n09.00 - 09.30", wed09Asd.deskripsi());
    }
}
