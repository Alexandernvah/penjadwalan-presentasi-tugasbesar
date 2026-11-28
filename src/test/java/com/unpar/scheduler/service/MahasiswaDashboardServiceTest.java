package com.unpar.scheduler.service;

import com.unpar.scheduler.config.AuthenticatedUserPrincipal;
import com.unpar.scheduler.dto.mahasiswa.MahasiswaDashboardDTO;
import com.unpar.scheduler.model.entity.Akun;
import com.unpar.scheduler.model.entity.Dosen;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kelompok;
import com.unpar.scheduler.model.entity.Mahasiswa;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.MataKuliahBuka;
import com.unpar.scheduler.model.entity.PesertaKelas;
import com.unpar.scheduler.model.entity.Presentasi;
import com.unpar.scheduler.model.entity.Role;
import com.unpar.scheduler.repository.AkunRepository;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KuliahRepository;
import com.unpar.scheduler.repository.MahasiswaRepository;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import com.unpar.scheduler.repository.PresentasiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MahasiswaDashboardServiceTest {

    private static final String SEMESTER = "26271";
    private static final String NPM = "6182201001";
    private static final LocalDateTime SEKARANG = LocalDateTime.of(2026, 10, 10, 12, 0);

    @Mock
    private KelasRepository kelasRepository;
    @Mock
    private PesertaKelasRepository pesertaKelasRepository;
    @Mock
    private PresentasiRepository presentasiRepository;
    @Mock
    private KuliahRepository kuliahRepository;
    @Mock
    private AkunRepository akunRepository;
    @Mock
    private MahasiswaRepository mahasiswaRepository;

    private MahasiswaDashboardService service;
    private MahasiswaResolver resolver;
    private Mahasiswa mahasiswa;
    private Kelas kelasAlgoritma;
    private Kelas kelasJaringan;

    @BeforeEach
    void setUp() {
        service = new MahasiswaDashboardService(
            kelasRepository, pesertaKelasRepository, presentasiRepository, new NamaDosenPengajarResolver(kuliahRepository)
        );
        resolver = new MahasiswaResolver(akunRepository, mahasiswaRepository);
        mahasiswa = new Mahasiswa(NPM, "Andi Pratama", new Akun());
        Dosen dosen = new Dosen("1975031501", "Dr. Budi Santoso", new Akun());
        kelasAlgoritma = new Kelas(101, new MataKuliahBuka(1, new MataKuliah("IF2110", "Algoritma dan Struktur Data", (short) 3), SEMESTER, dosen), "A", dosen);
        kelasJaringan = new Kelas(201, new MataKuliahBuka(2, new MataKuliah("IF3130", "Jaringan Komputer", (short) 3), SEMESTER, dosen), "B", dosen);
        lenient().when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.of(SEMESTER));
    }

    @Test
    void studentWithoutEnrollmentIsReportedAsNotYetInputAndSkipsPresentationLookup() {
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER)).thenReturn(List.of());

        MahasiswaDashboardDTO dashboard = service.getDashboard(mahasiswa, SEKARANG);

        assertFalse(dashboard.sudahInputMatkul());
        assertTrue(dashboard.daftarMatkulDiambil().isEmpty());
        assertNull(dashboard.jadwalPresentasiTerdekat());
        assertEquals("Andi Pratama", dashboard.namaMahasiswa());
        verify(presentasiRepository, never()).findByMahasiswaNpmAndSemester(NPM, SEMESTER);
    }

    @Test
    void enrolledStudentSeesLockedCoursesSortedByCodeWithoutPresentation() {
        enroll(kelasJaringan, kelasAlgoritma);
        when(presentasiRepository.findByMahasiswaNpmAndSemester(NPM, SEMESTER)).thenReturn(List.of());

        MahasiswaDashboardDTO dashboard = service.getDashboard(mahasiswa, SEKARANG);

        assertTrue(dashboard.sudahInputMatkul());
        assertEquals(List.of("IF2110", "IF3130"), dashboard.daftarMatkulDiambil().stream().map(m -> m.kodeMatkul()).toList());
        assertEquals("A", dashboard.daftarMatkulDiambil().get(0).kodeKelas());
        assertEquals("Dr. Budi Santoso", dashboard.daftarMatkulDiambil().get(0).namaDosen());
        assertNull(dashboard.jadwalPresentasiTerdekat());
    }

    @Test
    void nearestUpcomingPresentationIsPickedOverPastAndLaterOnes() {
        enroll(kelasAlgoritma, kelasJaringan);
        Presentasi sudahLewat = presentasi(kelasAlgoritma, 1, LocalDate.of(2026, 10, 7), LocalTime.of(10, 0), "Lab Komputer 1");
        Presentasi terdekat = presentasi(kelasJaringan, 4, LocalDate.of(2026, 10, 15), LocalTime.of(11, 0), "Lab Komputer 2");
        Presentasi lebihJauh = presentasi(kelasAlgoritma, 2, LocalDate.of(2026, 10, 20), LocalTime.of(9, 0), "Ruang 9122");
        when(presentasiRepository.findByMahasiswaNpmAndSemester(NPM, SEMESTER)).thenReturn(List.of(sudahLewat, lebihJauh, terdekat));

        var jadwal = service.getDashboard(mahasiswa, SEKARANG).jadwalPresentasiTerdekat();

        assertEquals("IF3130", jadwal.kodeMatkul());
        assertEquals((short) 4, jadwal.noKelompok());
        assertEquals("Lab Komputer 2", jadwal.ruangan());
        assertEquals(LocalTime.of(11, 30), jadwal.jamSelesai());
        assertEquals("Akan Datang", jadwal.status());
    }

    @Test
    void latestPresentationIsMarkedFinishedWhenAllHavePassed() {
        enroll(kelasAlgoritma);
        Presentasi awal = presentasi(kelasAlgoritma, 1, LocalDate.of(2026, 10, 1), LocalTime.of(10, 0), "Lab Komputer 1");
        Presentasi akhir = presentasi(kelasAlgoritma, 2, LocalDate.of(2026, 10, 7), LocalTime.of(10, 0), "Lab Komputer 1");
        when(presentasiRepository.findByMahasiswaNpmAndSemester(NPM, SEMESTER)).thenReturn(List.of(awal, akhir));

        var jadwal = service.getDashboard(mahasiswa, SEKARANG).jadwalPresentasiTerdekat();

        assertEquals((short) 2, jadwal.noKelompok());
        assertEquals("Selesai", jadwal.status());
    }

    @Test
    void presentationEndingExactlyNowIsStillUpcoming() {
        enroll(kelasAlgoritma);
        Presentasi berlangsung = presentasi(kelasAlgoritma, 3, SEKARANG.toLocalDate(), SEKARANG.toLocalTime().minusMinutes(30), "Lab Komputer 1");
        when(presentasiRepository.findByMahasiswaNpmAndSemester(NPM, SEMESTER)).thenReturn(List.of(berlangsung));

        assertEquals("Akan Datang", service.getDashboard(mahasiswa, SEKARANG).jadwalPresentasiTerdekat().status());
    }

    @Test
    void resolverMapsAuthenticationEmailThroughAkunToMahasiswaAndGuardsInvalidSessions() {
        Akun akun = new Akun(7, "6182201001@student.unpar.ac.id", "pw", Role.MAHASISWA, true);
        Mahasiswa resolved = new Mahasiswa(NPM, "Andi Pratama", akun);
        when(akunRepository.findByEmailIgnoreCase("6182201001@student.unpar.ac.id")).thenReturn(Optional.of(akun));
        when(mahasiswaRepository.findByAkun_IdAkun(7)).thenReturn(Optional.of(resolved));

        var validAuth = new UsernamePasswordAuthenticationToken(
            new AuthenticatedUserPrincipal(NPM, NPM, "Andi Pratama", "6182201001@student.unpar.ac.id", "MAHASISWA"),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_MAHASISWA"))
        );
        assertEquals(NPM, resolver.resolve(validAuth).getNpm());

        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(MahasiswaException.class, () -> resolver.resolve(null)).getStatus());

        var dosenAuth = new UsernamePasswordAuthenticationToken(
            new AuthenticatedUserPrincipal("1975031501", "1975031501", "Dr. Budi", "budi@unpar.ac.id", "DOSEN"),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_DOSEN"))
        );
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(MahasiswaException.class, () -> resolver.resolve(dosenAuth)).getStatus());

        when(mahasiswaRepository.findByAkun_IdAkun(7)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(MahasiswaException.class, () -> resolver.resolve(validAuth)).getStatus());
    }

    private void enroll(Kelas... daftarKelas) {
        List<PesertaKelas> peserta = java.util.Arrays.stream(daftarKelas)
            .map(kelas -> new PesertaKelas(kelas.getIdKelas(), mahasiswa, kelas)).toList();
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER)).thenReturn(peserta);
    }

    private Presentasi presentasi(Kelas kelas, int noKelompok, LocalDate tanggal, LocalTime mulai, String ruangan) {
        Kelompok kelompok = new Kelompok(noKelompok, kelas, (short) noKelompok, (short) 4, "Topik");
        return new Presentasi(noKelompok, kelompok, tanggal, mulai, mulai.plusMinutes(30), ruangan, false, null);
    }
}
