package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.jadwalpribadi.JadwalPresentasiItemDTO;
import com.unpar.scheduler.dto.jadwalpribadi.JadwalPribadiDosenResponseDTO;
import com.unpar.scheduler.dto.jadwalpribadi.JadwalRutinItemDTO;
import com.unpar.scheduler.dto.jadwalpribadi.JadwalUjianItemDTO;
import com.unpar.scheduler.dto.penjadwalan.AnggotaPenjadwalanDTO;
import com.unpar.scheduler.dto.penjadwalan.KelompokPenjadwalanDTO;
import com.unpar.scheduler.model.entity.Akun;
import com.unpar.scheduler.model.entity.AnggotaKelompok;
import com.unpar.scheduler.model.entity.Dosen;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kelompok;
import com.unpar.scheduler.model.entity.Kuliah;
import com.unpar.scheduler.model.entity.Mahasiswa;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.MataKuliahBuka;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JadwalPribadiDosenServiceTest {

    private static final String NIDN = "1975031501";
    private static final String SEMESTER = "25261";

    @Mock
    private DosenRepository dosenRepository;
    @Mock
    private KelasRepository kelasRepository;
    @Mock
    private KuliahRepository kuliahRepository;
    @Mock
    private PesertaKelasRepository pesertaKelasRepository;
    @Mock
    private PengawasUjianRepository pengawasUjianRepository;
    @Mock
    private PresentasiRepository presentasiRepository;
    @Mock
    private AnggotaKelompokRepository anggotaKelompokRepository;
    @Mock
    private PenjadwalanService penjadwalanService;

    private JadwalPribadiDosenService service;

    private Dosen dosenBudi;
    private MataKuliah mkJarkom;
    private MataKuliahBuka mkbJarkom;
    private Kelas kelasJarkom;
    private Mahasiswa mhsNurul;
    private Mahasiswa mhsAndi;

    @BeforeEach
    void setUp() {
        service = new JadwalPribadiDosenService(
            dosenRepository,
            kelasRepository,
            kuliahRepository,
            pesertaKelasRepository,
            pengawasUjianRepository,
            presentasiRepository,
            anggotaKelompokRepository,
            penjadwalanService
        );

        dosenBudi = new Dosen(NIDN, "Dr. Budi Santoso", new Akun());
        mkJarkom = new MataKuliah("IF3130", "Jaringan Komputer", (short) 3);
        mkbJarkom = new MataKuliahBuka(10, mkJarkom, SEMESTER, dosenBudi);
        kelasJarkom = new Kelas(102, mkbJarkom, "B", dosenBudi);
        mhsAndi = new Mahasiswa("6182101001", "Andi Saputra", new Akun());
        mhsNurul = new Mahasiswa("6182101002", "Nurul Hidayah", new Akun());
    }

    private void stubLecturerWithClass() {
        when(dosenRepository.findByNomorIndukDosen(NIDN)).thenReturn(Optional.of(dosenBudi));
        when(kelasRepository.findClassesByLecturerAndSemester(NIDN, SEMESTER)).thenReturn(List.of(kelasJarkom));
        when(kuliahRepository.findByKelas_IdKelasAndDosenPengajar_NomorIndukDosen(102, NIDN)).thenReturn(List.of(
            new Kuliah(1, kelasJarkom, dosenBudi, (short) 5, LocalTime.of(13, 0), LocalTime.of(15, 0), "Ruang 9013", "KULIAH"),
            new Kuliah(2, kelasJarkom, dosenBudi, (short) 1, LocalTime.of(13, 0), LocalTime.of(15, 0), "Ruang 9013", "KULIAH"),
            new Kuliah(3, kelasJarkom, dosenBudi, (short) 3, LocalTime.of(13, 0), LocalTime.of(15, 0), "Ruang 9013", "KULIAH")
        ));
        when(pesertaKelasRepository.findByKelasIdsWithMahasiswa(anyCollection())).thenReturn(List.of(
            new PesertaKelas(2, mhsNurul, kelasJarkom),
            new PesertaKelas(1, mhsAndi, kelasJarkom)
        ));
        when(pengawasUjianRepository.findByDosenAndSemesterWithDetails(NIDN, SEMESTER)).thenReturn(Collections.emptyList());
        when(presentasiRepository.findByKelasIdsWithDetails(anyCollection())).thenReturn(Collections.emptyList());
    }

    @Test
    void routineCourseContainsScheduleRoomAndSortedStudents() {
        stubLecturerWithClass();

        JadwalPribadiDosenResponseDTO response = service.getJadwalPribadi(NIDN);

        assertEquals(1, response.jadwalRutin().size());
        JadwalRutinItemDTO rutin = response.jadwalRutin().get(0);
        assertEquals(102, rutin.idKelas());
        assertEquals("IF3130", rutin.kodeMatkul());
        assertEquals("Jaringan Komputer", rutin.namaMatkul());
        assertEquals("Kelas B", rutin.namaKelas());
        assertEquals("Senin, Rabu, Jumat (13:00 - 15:00)", rutin.jadwalRingkas());
        assertEquals("Ruang 9013", rutin.ruangan());
        assertEquals(2, rutin.jumlahMahasiswa());
        assertEquals("6182101001", rutin.daftarMahasiswa().get(0).npm());
        assertEquals("Andi Saputra", rutin.daftarMahasiswa().get(0).nama());
        assertEquals("6182101002", rutin.daftarMahasiswa().get(1).npm());
        assertEquals("Nurul Hidayah", rutin.daftarMahasiswa().get(1).nama());
    }

    @Test
    void routineCourseWithDifferentTimesListsEachTimeRange() {
        stubLecturerWithClass();
        when(kuliahRepository.findByKelas_IdKelasAndDosenPengajar_NomorIndukDosen(102, NIDN)).thenReturn(List.of(
            new Kuliah(1, kelasJarkom, dosenBudi, (short) 1, LocalTime.of(8, 0), LocalTime.of(10, 0), "Ruang 9013", "KULIAH"),
            new Kuliah(2, kelasJarkom, dosenBudi, (short) 3, LocalTime.of(13, 0), LocalTime.of(15, 0), "Lab Jaringan", "PRAKTIKUM")
        ));

        JadwalRutinItemDTO rutin = service.getJadwalPribadi(NIDN).jadwalRutin().get(0);

        assertEquals("Senin (08:00 - 10:00); Rabu (13:00 - 15:00)", rutin.jadwalRingkas());
        assertEquals("Ruang 9013, Lab Jaringan", rutin.ruangan());
    }

    @Test
    void examSupervisionSchedulesAreMappedAndSortedByDate() {
        stubLecturerWithClass();
        MataKuliah mkAlpro = new MataKuliah("IF1110", "Algoritma Pemrograman", (short) 4);
        MataKuliahBuka mkbAlpro = new MataKuliahBuka(11, mkAlpro, SEMESTER, dosenBudi);
        Ujian uas = new Ujian(2, mkbJarkom, "UAS", LocalDate.of(2026, 12, 9),
            LocalTime.of(10, 0), LocalTime.of(12, 0), "Ruang 9014");
        Ujian uts = new Ujian(1, mkbAlpro, "UTS", LocalDate.of(2026, 10, 19),
            LocalTime.of(8, 0), LocalTime.of(10, 0), "Ruang 9012");
        when(pengawasUjianRepository.findByDosenAndSemesterWithDetails(NIDN, SEMESTER)).thenReturn(List.of(
            new PengawasUjian(uas, dosenBudi),
            new PengawasUjian(uts, dosenBudi)
        ));

        List<JadwalUjianItemDTO> ujian = service.getJadwalPribadi(NIDN).jadwalUjian();

        assertEquals(2, ujian.size());
        JadwalUjianItemDTO first = ujian.get(0);
        assertEquals(1, first.idUjian());
        assertEquals("UTS", first.jenisUjian());
        assertEquals("2026-10-19", first.tanggal());
        assertEquals("08:00", first.waktuMulai());
        assertEquals("10:00", first.waktuSelesai());
        assertEquals("IF1110", first.kodeMatkul());
        assertEquals("Algoritma Pemrograman", first.namaMatkul());
        assertEquals("Ruang 9012", first.ruangan());
        assertEquals("UAS", ujian.get(1).jenisUjian());
        assertEquals("2026-12-09", ujian.get(1).tanggal());
    }

    @Test
    void scheduledPresentationsIncludeGroupAndSortedMembers() {
        stubLecturerWithClass();
        Kelompok kelompok = new Kelompok(5, kelasJarkom, (short) 2, (short) 4, "Perancangan Topologi Jaringan Kampus");
        Presentasi presentasi = new Presentasi(
            7, kelompok, LocalDate.of(2026, 10, 6),
            LocalTime.of(9, 0), LocalTime.of(9, 30), "Ruang 9122", false, null
        );
        when(presentasiRepository.findByKelasIdsWithDetails(anyCollection())).thenReturn(List.of(presentasi));
        when(anggotaKelompokRepository.findByKelompokIdsWithMahasiswa(anyCollection())).thenReturn(List.of(
            new AnggotaKelompok(2, mhsNurul, kelompok, kelasJarkom),
            new AnggotaKelompok(1, mhsAndi, kelompok, kelasJarkom)
        ));

        List<JadwalPresentasiItemDTO> list = service.getJadwalPribadi(NIDN).jadwalPresentasi();

        assertEquals(1, list.size());
        JadwalPresentasiItemDTO item = list.get(0);
        assertEquals(7, item.idPresentasi());
        assertEquals("2026-10-06", item.tanggal());
        assertEquals("09:00", item.waktuMulai());
        assertEquals("09:30", item.waktuSelesai());
        assertEquals("IF3130", item.kodeMatkul());
        assertEquals("Jaringan Komputer", item.namaMatkul());
        assertEquals("Kelompok 2", item.namaKelompok());
        assertEquals("Perancangan Topologi Jaringan Kampus", item.topik());
        assertEquals("Ruang 9122", item.ruangan());
        assertEquals(2, item.anggotaKelompok().size());
        assertEquals("6182101001", item.anggotaKelompok().get(0).npm());
        assertEquals("Andi Saputra", item.anggotaKelompok().get(0).nama());
        assertEquals("Nurul Hidayah", item.anggotaKelompok().get(1).nama());
    }

    @Test
    void unknownLecturerFallsBackToSampleDataWithAllSections() {
        when(dosenRepository.findByNomorIndukDosen(NIDN)).thenReturn(Optional.empty());
        when(penjadwalanService.getKelompokByKelas(eq(102))).thenReturn(Collections.emptyList());
        when(penjadwalanService.getKelompokByKelas(eq(101))).thenReturn(Collections.emptyList());

        JadwalPribadiDosenResponseDTO response = service.getJadwalPribadi(null);

        assertFalse(response.jadwalRutin().isEmpty());
        JadwalRutinItemDTO jarkom = response.jadwalRutin().get(0);
        assertEquals("IF3130", jarkom.kodeMatkul());
        assertEquals("Kelas B", jarkom.namaKelas());
        assertEquals("Senin, Rabu, Jumat (13:00 - 15:00)", jarkom.jadwalRingkas());
        assertEquals(24, jarkom.jumlahMahasiswa());
        assertEquals(24, jarkom.daftarMahasiswa().size());
        assertTrue(jarkom.daftarMahasiswa().get(0).npm().compareTo(jarkom.daftarMahasiswa().get(1).npm()) < 0);
        assertFalse(response.jadwalUjian().isEmpty());
        assertEquals("UTS", response.jadwalUjian().get(0).jenisUjian());
        assertEquals(1, response.jadwalPresentasi().size());
    }

    @Test
    void fallbackModeShowsPresentationsScheduledThroughPenjadwalan() {
        when(dosenRepository.findByNomorIndukDosen(NIDN)).thenReturn(Optional.empty());
        when(penjadwalanService.getKelompokByKelas(102)).thenReturn(List.of(
            new KelompokPenjadwalanDTO(
                2, 2, 2, "Perancangan Topologi Jaringan Kampus", 4, 30, true, 901,
                "Rabu", "2026-10-07", "13:00", "13:30", "Ruang 9122", null,
                List.of(
                    new AnggotaPenjadwalanDTO("6182101002", "Nurul Hidayah", false),
                    new AnggotaPenjadwalanDTO("6182101001", "Andi Saputra", true)
                )
            ),
            new KelompokPenjadwalanDTO(
                3, 3, 3, "Analisis Kinerja Protokol Routing OSPF", 5, 30, false, null,
                null, null, null, null, null, null, Collections.emptyList()
            )
        ));
        when(penjadwalanService.getKelompokByKelas(101)).thenReturn(Collections.emptyList());

        List<JadwalPresentasiItemDTO> list = service.getJadwalPribadi(NIDN).jadwalPresentasi();

        assertEquals(1, list.size());
        JadwalPresentasiItemDTO item = list.get(0);
        assertEquals(901, item.idPresentasi());
        assertEquals("2026-10-07", item.tanggal());
        assertEquals("IF3130", item.kodeMatkul());
        assertEquals("Kelompok 2", item.namaKelompok());
        assertNotNull(item.anggotaKelompok());
        assertEquals("6182101001", item.anggotaKelompok().get(0).npm());
    }

    @Test
    void lecturerWithoutClassesFallsBackToSampleData() {
        when(dosenRepository.findByNomorIndukDosen(NIDN)).thenReturn(Optional.of(dosenBudi));
        when(kelasRepository.findClassesByLecturerAndSemester(NIDN, SEMESTER)).thenReturn(Collections.emptyList());
        when(penjadwalanService.getKelompokByKelas(anyInt())).thenReturn(Collections.emptyList());

        JadwalPribadiDosenResponseDTO response = service.getJadwalPribadi(NIDN);

        assertEquals(2, response.jadwalRutin().size());
        assertEquals(3, response.jadwalUjian().size());
    }
}
