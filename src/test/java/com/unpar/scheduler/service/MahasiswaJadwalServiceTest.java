package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.mahasiswa.JadwalPresentasiMahasiswaDTO;
import com.unpar.scheduler.dto.mahasiswa.MahasiswaJadwalResponseDTO;
import com.unpar.scheduler.model.entity.Akun;
import com.unpar.scheduler.model.entity.AnggotaKelompok;
import com.unpar.scheduler.model.entity.Dosen;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kelompok;
import com.unpar.scheduler.model.entity.Mahasiswa;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.MataKuliahBuka;
import com.unpar.scheduler.model.entity.PesertaKelas;
import com.unpar.scheduler.model.entity.Presentasi;
import com.unpar.scheduler.repository.AnggotaKelompokRepository;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KuliahRepository;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import com.unpar.scheduler.repository.PresentasiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MahasiswaJadwalServiceTest {

    private static final String SEMESTER = "26271";
    private static final String NPM = "6182201001";
    private static final LocalDate HARI_INI = LocalDate.of(2026, 10, 15);

    @Mock
    private KelasRepository kelasRepository;
    @Mock
    private PesertaKelasRepository pesertaKelasRepository;
    @Mock
    private PresentasiRepository presentasiRepository;
    @Mock
    private AnggotaKelompokRepository anggotaKelompokRepository;
    @Mock
    private KuliahRepository kuliahRepository;

    private MahasiswaJadwalService service;
    private Mahasiswa currentStudent;
    private Mahasiswa teammateA;
    private Mahasiswa teammateZ;
    private Kelas kelasAlgoritma;
    private Kelas kelasJaringan;
    private Kelas kelasPbo;

    @BeforeEach
    void setUp() {
        SemesterAkademikResolver semesterResolver = new SemesterAkademikResolver(kelasRepository, pesertaKelasRepository);
        service = new MahasiswaJadwalService(
            semesterResolver,
            pesertaKelasRepository,
            presentasiRepository,
            anggotaKelompokRepository,
            new NamaDosenPengajarResolver(kuliahRepository)
        );

        currentStudent = new Mahasiswa(NPM, "Dimas Pratama", new Akun());
        teammateA = new Mahasiswa("6182201002", "Andi Wijaya", new Akun());
        teammateZ = new Mahasiswa("6182201003", "Zaki Akbar", new Akun());

        Dosen dosenBudi = new Dosen("1975031501", "Dr. Budi Santoso", new Akun());
        Dosen dosenYuni = new Dosen("1982091203", "Dr. Yuni Kartika", new Akun());

        kelasAlgoritma = new Kelas(
            101,
            new MataKuliahBuka(1, new MataKuliah("IF2110", "Algoritma dan Struktur Data", (short) 4), SEMESTER, dosenBudi),
            "A",
            dosenBudi
        );
        kelasJaringan = new Kelas(
            102,
            new MataKuliahBuka(2, new MataKuliah("IF3130", "Jaringan Komputer", (short) 3), SEMESTER, dosenBudi),
            "B",
            dosenBudi
        );
        kelasPbo = new Kelas(
            103,
            new MataKuliahBuka(3, new MataKuliah("IF2210", "Pemrograman Berorientasi Objek", (short) 4), SEMESTER, dosenYuni),
            "A",
            dosenYuni
        );

        lenient().when(pesertaKelasRepository.findLatestSemesterByMahasiswaNpm(NPM)).thenReturn(Optional.of(SEMESTER));
    }

    @Test
    void studentWithoutEnrollmentGetsEmptySchedule() {
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER)).thenReturn(List.of());

        MahasiswaJadwalResponseDTO res = service.getJadwal(currentStudent, HARI_INI);

        assertEquals("Dimas Pratama", res.namaMahasiswa());
        assertEquals(NPM, res.npm());
        assertEquals("Semester Ganjil 2026/2027", res.semesterAkademikLabel());
        assertTrue(res.daftarJadwal().isEmpty());
        verifyNoInteractions(presentasiRepository);
    }

    @Test
    void schedulesAreSortedChronologicallyWithStatusesDatesMembersAndNullableFields() {
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER)).thenReturn(List.of(
            new PesertaKelas(1, currentStudent, kelasAlgoritma),
            new PesertaKelas(2, currentStudent, kelasJaringan),
            new PesertaKelas(3, currentStudent, kelasPbo)
        ));

        Kelompok kelPast = new Kelompok(1, kelasJaringan, (short) 2, (short) 3, "Topologi Kampus", (short) 30);
        Kelompok kelToday = new Kelompok(2, kelasAlgoritma, (short) 4, (short) 4, "Pohon Berimbang", (short) 30);
        Kelompok kelFuture = new Kelompok(3, kelasPbo, (short) 1, (short) 3, "   ", (short) 30);

        Presentasi presFuture = new Presentasi(
            30, kelFuture, LocalDate.of(2026, 11, 5), LocalTime.of(9, 0), LocalTime.of(9, 30), " ", false, null
        );
        Presentasi presPast = new Presentasi(
            10, kelPast, LocalDate.of(2026, 10, 7), LocalTime.of(13, 0), LocalTime.of(13, 30), "Ruang 9122", false, "   "
        );
        Presentasi presToday = new Presentasi(
            20, kelToday, HARI_INI, LocalTime.of(11, 0), LocalTime.of(11, 30), "Lab Komputer 1", false, "Bawa laptop demo"
        );

        // Intentionally unsorted to verify service-level chronological ordering
        when(presentasiRepository.findByMahasiswaNpmAndSemester(NPM, SEMESTER))
            .thenReturn(List.of(presFuture, presPast, presToday));
        when(anggotaKelompokRepository.findByKelompokIdsWithMahasiswa(List.of(1, 2, 3))).thenReturn(List.of(
            new AnggotaKelompok(1, teammateZ, kelToday, kelasAlgoritma),
            new AnggotaKelompok(2, currentStudent, kelToday, kelasAlgoritma),
            new AnggotaKelompok(3, teammateA, kelToday, kelasAlgoritma),
            new AnggotaKelompok(4, currentStudent, kelPast, kelasJaringan),
            new AnggotaKelompok(5, currentStudent, kelFuture, kelasPbo)
        ));

        MahasiswaJadwalResponseDTO res = service.getJadwal(currentStudent, HARI_INI);

        assertEquals(3, res.daftarJadwal().size());

        JadwalPresentasiMahasiswaDTO first = res.daftarJadwal().get(0);
        assertEquals(10, first.idPresentasi());
        assertEquals("SELESAI", first.statusKode());
        assertEquals("Selesai", first.statusLabel());
        assertEquals("07", first.tanggalAngka());
        assertEquals("OKTOBER 2026", first.bulanTahunLabel());
        assertEquals("Rabu, 7 Oktober 2026", first.hariTanggalFormatted());

        JadwalPresentasiMahasiswaDTO second = res.daftarJadwal().get(1);
        assertEquals(20, second.idPresentasi());
        assertEquals("HARI_INI", second.statusKode());
        assertEquals("Hari Ini", second.statusLabel());
        assertEquals("Kamis, 15 Oktober 2026", second.hariTanggalFormatted());
        assertEquals("15", second.tanggalAngka());
        assertEquals("11:00 - 11:30 WIB", second.rentangWaktuLabel());
        assertEquals("Dr. Budi Santoso", second.namaDosen());
        assertEquals("Lab Komputer 1", second.ruangan());
        assertEquals(3, second.daftarAnggota().size());
        assertTrue(second.daftarAnggota().get(0).isCurrentUser());
        assertEquals("Dimas Pratama", second.daftarAnggota().get(0).nama());
        assertEquals("Andi Wijaya", second.daftarAnggota().get(1).nama());
        assertEquals("Zaki Akbar", second.daftarAnggota().get(2).nama());

        JadwalPresentasiMahasiswaDTO third = res.daftarJadwal().get(2);
        assertEquals(30, third.idPresentasi());
        assertEquals("AKAN_DATANG", third.statusKode());
        assertEquals("Akan Datang", third.statusLabel());
        assertEquals("NOVEMBER 2026", third.bulanTahunLabel());
        assertNull(third.topik());
        assertNull(third.ruangan());
    }
}
