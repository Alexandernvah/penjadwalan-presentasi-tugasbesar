package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.mahasiswa.KelasTersediaDTO;
import com.unpar.scheduler.dto.mahasiswa.MatkulMahasiswaResponseDTO;
import com.unpar.scheduler.model.entity.Akun;
import com.unpar.scheduler.model.entity.Dosen;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kuliah;
import com.unpar.scheduler.model.entity.Mahasiswa;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.MataKuliahBuka;
import com.unpar.scheduler.model.entity.PesertaKelas;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KuliahRepository;
import com.unpar.scheduler.repository.MahasiswaRepository;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MahasiswaMatkulServiceTest {

    private static final String SEMESTER = "26271";
    private static final String NPM = "6182201001";

    @Mock
    private KelasRepository kelasRepository;
    @Mock
    private PesertaKelasRepository pesertaKelasRepository;
    @Mock
    private MahasiswaRepository mahasiswaRepository;
    @Mock
    private KuliahRepository kuliahRepository;

    private MahasiswaMatkulService service;
    private Mahasiswa mahasiswa;
    private Dosen penanggungJawab;
    private Dosen koordinator;
    private MataKuliahBuka algoritmaBuka;
    private MataKuliahBuka jaringanBuka;
    private Kelas algoritmaA;
    private Kelas algoritmaB;
    private Kelas jaringanA;

    @BeforeEach
    void setUp() {
        SemesterAkademikResolver semesterResolver = new SemesterAkademikResolver(kelasRepository, pesertaKelasRepository);
        service = new MahasiswaMatkulService(
            kelasRepository,
            pesertaKelasRepository,
            mahasiswaRepository,
            new NamaDosenPengajarResolver(kuliahRepository),
            semesterResolver
        );
        mahasiswa = new Mahasiswa(NPM, "Andi Pratama", new Akun());
        penanggungJawab = new Dosen("1975031501", "Dr. Budi Santoso", new Akun());
        koordinator = new Dosen("1980010101", "Dr. Citra Lestari", new Akun());

        algoritmaBuka = new MataKuliahBuka(1, new MataKuliah("IF2110", "Algoritma dan Struktur Data", (short) 3), SEMESTER, koordinator);
        jaringanBuka = new MataKuliahBuka(2, new MataKuliah("IF3130", "Jaringan Komputer", (short) 3), SEMESTER, koordinator);
        algoritmaA = new Kelas(101, algoritmaBuka, "A", penanggungJawab);
        algoritmaB = new Kelas(102, algoritmaBuka, "B", null);
        jaringanA = new Kelas(201, jaringanBuka, "A", null);
    }

    @Test
    void getMatkulTersediaWithoutEnrollmentIsUnlockedAndGroupedByCourse() {
        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.of(SEMESTER));
        when(kelasRepository.findBySemester(SEMESTER)).thenReturn(List.of(jaringanA, algoritmaA, algoritmaB));
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER)).thenReturn(List.of());
        when(kuliahRepository.findByKelasIdsWithDetails(any())).thenReturn(List.of());

        MatkulMahasiswaResponseDTO response = service.getMatkulTersedia(mahasiswa);

        assertEquals("Andi Pratama", response.namaMahasiswa());
        assertEquals("Semester Ganjil 2026/2027", response.semesterAkademikLabel());
        assertFalse(response.sudahTerkunci());
        assertEquals(List.of("IF2110", "IF3130"), response.daftarMataKuliah().stream().map(m -> m.kodeMatkul()).toList());
        assertEquals(2, response.daftarMataKuliah().get(0).daftarKelas().size());
    }

    @Test
    void teacherNameFallsBackFromResponsiblePersonToLectureTeacherToCoordinator() {
        Dosen pengajarKuliah = new Dosen("1990020202", "Dr. Dewi Anggraini", new Akun());
        Kuliah kuliahB = new Kuliah(1, algoritmaB, pengajarKuliah, (short) 1, LocalTime.of(8, 0), LocalTime.of(10, 0), "Ruang 9013", "KULIAH");
        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.of(SEMESTER));
        when(kelasRepository.findBySemester(SEMESTER)).thenReturn(List.of(algoritmaA, algoritmaB, jaringanA));
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER)).thenReturn(List.of());
        when(kuliahRepository.findByKelasIdsWithDetails(any())).thenReturn(List.of(kuliahB));

        List<KelasTersediaDTO> algoritma = service.getMatkulTersedia(mahasiswa).daftarMataKuliah().get(0).daftarKelas();
        List<KelasTersediaDTO> jaringan = service.getMatkulTersedia(mahasiswa).daftarMataKuliah().get(1).daftarKelas();

        assertEquals("Dr. Budi Santoso", algoritma.get(0).namaDosenPengajar());
        assertEquals("Dr. Dewi Anggraini", algoritma.get(1).namaDosenPengajar());
        assertEquals("Dr. Citra Lestari", jaringan.get(0).namaDosenPengajar());
    }

    @Test
    void getMatkulTersediaMarksChosenClassesWhenAlreadyLocked() {
        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.of(SEMESTER));
        when(kelasRepository.findBySemester(SEMESTER)).thenReturn(List.of(algoritmaA, algoritmaB));
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER))
            .thenReturn(List.of(new PesertaKelas(1, mahasiswa, algoritmaB)));

        MatkulMahasiswaResponseDTO response = service.getMatkulTersedia(mahasiswa);

        assertTrue(response.sudahTerkunci());
        List<KelasTersediaDTO> kelas = response.daftarMataKuliah().get(0).daftarKelas();
        assertFalse(kelas.get(0).dipilih());
        assertTrue(kelas.get(1).dipilih());
    }

    @Test
    void simpanMatkulPersistsEverySelectedClassAndReturnsLockedState() {
        stubSimpanPreconditions();
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER))
            .thenReturn(List.of(), List.of(new PesertaKelas(1, mahasiswa, algoritmaA), new PesertaKelas(2, mahasiswa, jaringanA)));
        when(kelasRepository.findByIdsWithDetails(any())).thenReturn(List.of(algoritmaA, jaringanA));
        when(kelasRepository.findBySemester(SEMESTER)).thenReturn(List.of(algoritmaA, jaringanA));

        MatkulMahasiswaResponseDTO response = service.simpanMatkul(mahasiswa, List.of(101, 201));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<PesertaKelas>> saved = ArgumentCaptor.forClass(Iterable.class);
        verify(pesertaKelasRepository).saveAll(saved.capture());
        assertEquals(List.of(101, 201), java.util.stream.StreamSupport.stream(saved.getValue().spliterator(), false)
            .map(peserta -> peserta.getKelas().getIdKelas()).toList());
        assertTrue(response.sudahTerkunci());
    }

    @Test
    void simpanMatkulRejectsEditAfterLockWithConflict() {
        stubSimpanPreconditions();
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER))
            .thenReturn(List.of(new PesertaKelas(1, mahasiswa, algoritmaA)));

        MahasiswaException error = assertThrows(MahasiswaException.class, () -> service.simpanMatkul(mahasiswa, List.of(201)));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(pesertaKelasRepository, never()).saveAll(any());
    }

    @Test
    void simpanMatkulRejectsEmptySelection() {
        stubSimpanPreconditions();
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER)).thenReturn(List.of());

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(MahasiswaException.class, () -> service.simpanMatkul(mahasiswa, List.of())).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(MahasiswaException.class, () -> service.simpanMatkul(mahasiswa, null)).getStatus());
        verify(pesertaKelasRepository, never()).saveAll(any());
    }

    @Test
    void simpanMatkulRejectsTwoClassesOfTheSameCourse() {
        stubSimpanPreconditions();
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER)).thenReturn(List.of());
        when(kelasRepository.findByIdsWithDetails(any())).thenReturn(List.of(algoritmaA, algoritmaB));

        MahasiswaException error = assertThrows(MahasiswaException.class, () -> service.simpanMatkul(mahasiswa, List.of(101, 102)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verify(pesertaKelasRepository, never()).saveAll(any());
    }

    @Test
    void simpanMatkulRejectsDuplicateAndUnknownClassIds() {
        stubSimpanPreconditions();
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER)).thenReturn(List.of());
        when(kelasRepository.findByIdsWithDetails(any())).thenReturn(List.of(algoritmaA));

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(MahasiswaException.class, () -> service.simpanMatkul(mahasiswa, List.of(101, 101))).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(MahasiswaException.class, () -> service.simpanMatkul(mahasiswa, List.of(101, 999))).getStatus());
        verify(pesertaKelasRepository, never()).saveAll(any());
    }

    @Test
    void getMatkulTersediaFormatsDynamicAcademicSemesterAndHandlesEmptyTable() {
        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.of("20272"));
        when(kelasRepository.findBySemester("20272")).thenReturn(List.of());
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, "20272")).thenReturn(List.of());

        MatkulMahasiswaResponseDTO activeResponse = service.getMatkulTersedia(mahasiswa);
        assertEquals("Semester Genap 2027/2028", activeResponse.semesterAkademikLabel());

        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.empty());
        MatkulMahasiswaResponseDTO emptyResponse = service.getMatkulTersedia(mahasiswa);
        assertEquals("Semester Belum Tersedia", emptyResponse.semesterAkademikLabel());
        assertTrue(emptyResponse.daftarMataKuliah().isEmpty());
    }

    private void stubSimpanPreconditions() {
        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.of(SEMESTER));
        when(mahasiswaRepository.findByNpmForUpdate(NPM)).thenReturn(Optional.of(mahasiswa));
    }
}
