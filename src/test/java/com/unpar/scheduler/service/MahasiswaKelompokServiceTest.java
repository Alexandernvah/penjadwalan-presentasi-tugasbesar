package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.mahasiswa.DaftarKelompokKelasResponseDTO;
import com.unpar.scheduler.dto.mahasiswa.KelompokMataKuliahResponseDTO;
import com.unpar.scheduler.dto.mahasiswa.KelompokTersediaDTO;
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
import com.unpar.scheduler.repository.KelompokRepository;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import com.unpar.scheduler.repository.PresentasiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MahasiswaKelompokServiceTest {

    private static final String SEMESTER = "26271";
    private static final String NPM = "6182201001";

    @Mock
    private PesertaKelasRepository pesertaKelasRepository;
    @Mock
    private KelasRepository kelasRepository;
    @Mock
    private KelompokRepository kelompokRepository;
    @Mock
    private AnggotaKelompokRepository anggotaKelompokRepository;
    @Mock
    private PresentasiRepository presentasiRepository;

    private MahasiswaKelompokService service;
    private Mahasiswa currentStudent;
    private Mahasiswa teammateA;
    private Mahasiswa teammateZ;
    private Kelas kelasAlgoritma;
    private Kelas kelasJaringan;
    private Kelompok kelompok1;
    private Kelompok kelompok2;
    private Kelompok kelompok3;

    @BeforeEach
    void setUp() {
        SemesterAkademikResolver semesterResolver = new SemesterAkademikResolver(kelasRepository, pesertaKelasRepository);
        service = new MahasiswaKelompokService(
            semesterResolver,
            pesertaKelasRepository,
            kelasRepository,
            kelompokRepository,
            anggotaKelompokRepository,
            presentasiRepository
        );

        currentStudent = new Mahasiswa(NPM, "Dimas Pratama", new Akun());
        teammateA = new Mahasiswa("6182201002", "Andi Wijaya", new Akun());
        teammateZ = new Mahasiswa("6182201003", "Zaki Akbar", new Akun());

        Dosen dosen = new Dosen("1975031501", "Dr. Budi Santoso", new Akun());
        kelasAlgoritma = new Kelas(
            101,
            new MataKuliahBuka(1, new MataKuliah("IF2110", "Algoritma dan Struktur Data", (short) 4), SEMESTER, dosen),
            "A",
            dosen
        );
        kelasJaringan = new Kelas(
            102,
            new MataKuliahBuka(2, new MataKuliah("IF3130", "Jaringan Komputer", (short) 3), SEMESTER, dosen),
            "B",
            dosen
        );

        kelompok1 = new Kelompok(1, kelasAlgoritma, (short) 1, (short) 2, "Pohon Berimbang", (short) 30);
        kelompok2 = new Kelompok(2, kelasAlgoritma, (short) 2, (short) 1, "Graf Terpendek", (short) 30);
        kelompok3 = new Kelompok(3, kelasAlgoritma, (short) 3, (short) 3, null, (short) 30);

        lenient().when(pesertaKelasRepository.findLatestSemesterByMahasiswaNpm(NPM)).thenReturn(Optional.of(SEMESTER));
    }

    @Test
    void getMataKuliahDiambilReturnsSortedEnrolledClassesAndHandlesEmpty() {
        when(pesertaKelasRepository.findByNpmAndSemesterWithDetails(NPM, SEMESTER))
            .thenReturn(List.of(
                new PesertaKelas(2, currentStudent, kelasJaringan),
                new PesertaKelas(1, currentStudent, kelasAlgoritma)
            ));

        KelompokMataKuliahResponseDTO res = service.getMataKuliahDiambil(currentStudent);
        assertEquals("Dimas Pratama", res.namaMahasiswa());
        assertEquals(2, res.daftarKelasDiambil().size());
        assertEquals("IF2110 - Algoritma dan Struktur Data (Kelas A)", res.daftarKelasDiambil().get(0).label());
        assertEquals("IF3130 - Jaringan Komputer (Kelas B)", res.daftarKelasDiambil().get(1).label());

        when(pesertaKelasRepository.findLatestSemesterByMahasiswaNpm(NPM)).thenReturn(Optional.empty());
        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.empty());
        assertTrue(service.getMataKuliahDiambil(currentStudent).daftarKelasDiambil().isEmpty());
    }

    @Test
    void getDaftarKelompokMapsStatusesMemberOrderAndScheduledFlag() {
        when(kelasRepository.findByIdWithDetails(101)).thenReturn(Optional.of(kelasAlgoritma));
        when(pesertaKelasRepository.existsByMahasiswa_NpmAndKelas_IdKelas(NPM, 101)).thenReturn(true);
        when(kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(101))
            .thenReturn(List.of(kelompok1, kelompok2, kelompok3));
        when(anggotaKelompokRepository.findByKelompokIdsWithMahasiswa(List.of(1, 2, 3)))
            .thenReturn(List.of(
                new AnggotaKelompok(10, teammateZ, kelompok1, kelasAlgoritma),
                new AnggotaKelompok(11, currentStudent, kelompok1, kelasAlgoritma),
                new AnggotaKelompok(12, teammateA, kelompok2, kelasAlgoritma)
            ));
        Presentasi pres = new Presentasi(
            1, kelompok1, LocalDate.of(2026, 10, 7), LocalTime.of(10, 0), LocalTime.of(10, 30), "Lab 1", false, null
        );
        when(presentasiRepository.findByKelasIdWithDetails(101)).thenReturn(List.of(pres));

        DaftarKelompokKelasResponseDTO dto = service.getDaftarKelompok(currentStudent, 101);

        assertEquals(1, dto.idKelompokTerdaftar());
        assertEquals((short) 1, dto.noKelompokTerdaftar());
        assertEquals(3, dto.daftarKelompok().size());

        KelompokTersediaDTO k1 = dto.daftarKelompok().get(0);
        assertEquals("Kelompok Anda", k1.statusLabel());
        assertTrue(k1.isAnggotaSaya());
        assertTrue(k1.isPenuh());
        assertTrue(k1.sudahTerjadwal());
        assertTrue(k1.daftarAnggota().get(0).isCurrentUser());
        assertEquals("Zaki Akbar", k1.daftarAnggota().get(1).nama());

        KelompokTersediaDTO k2 = dto.daftarKelompok().get(1);
        assertEquals("Penuh", k2.statusLabel());
        assertFalse(k2.isAnggotaSaya());
        assertTrue(k2.isPenuh());

        KelompokTersediaDTO k3 = dto.daftarKelompok().get(2);
        assertEquals("Tersedia", k3.statusLabel());
        assertFalse(k3.isPenuh());
        assertTrue(k3.daftarAnggota().isEmpty());
    }

    @Test
    void getDaftarKelompokRejectsUnenrolledStudentWithForbidden() {
        when(kelasRepository.findByIdWithDetails(101)).thenReturn(Optional.of(kelasAlgoritma));
        when(pesertaKelasRepository.existsByMahasiswa_NpmAndKelas_IdKelas(NPM, 101)).thenReturn(false);

        MahasiswaException err = assertThrows(MahasiswaException.class, () -> service.getDaftarKelompok(currentStudent, 101));
        assertEquals(HttpStatus.FORBIDDEN, err.getStatus());
    }

    @Test
    void gabungKelompokPersistsMembershipAndReturnsUpdatedClassState() {
        when(kelompokRepository.findByIdForUpdate(3)).thenReturn(Optional.of(kelompok3));
        when(pesertaKelasRepository.existsByMahasiswa_NpmAndKelas_IdKelas(NPM, 101)).thenReturn(true);
        when(anggotaKelompokRepository.existsByMahasiswa_NpmAndKelas_IdKelas(NPM, 101)).thenReturn(false);
        when(anggotaKelompokRepository.countByKelompok_IdKelompok(3)).thenReturn(0L);
        when(kelasRepository.findByIdWithDetails(101)).thenReturn(Optional.of(kelasAlgoritma));
        when(kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(101)).thenReturn(List.of(kelompok3));
        when(anggotaKelompokRepository.findByKelompokIdsWithMahasiswa(List.of(3)))
            .thenReturn(List.of(new AnggotaKelompok(99, currentStudent, kelompok3, kelasAlgoritma)));
        when(presentasiRepository.findByKelasIdWithDetails(101)).thenReturn(List.of());

        DaftarKelompokKelasResponseDTO updated = service.gabungKelompok(currentStudent, 3);

        ArgumentCaptor<AnggotaKelompok> saved = ArgumentCaptor.forClass(AnggotaKelompok.class);
        verify(anggotaKelompokRepository).saveAndFlush(saved.capture());
        assertEquals(3, saved.getValue().getKelompok().getIdKelompok());
        assertEquals(101, saved.getValue().getKelas().getIdKelas());
        assertEquals(3, updated.idKelompokTerdaftar());
        assertEquals("Kelompok Anda", updated.daftarKelompok().get(0).statusLabel());
    }

    @Test
    void gabungKelompokRejectsWhenFullOrAlreadyInSameClass() {
        when(kelompokRepository.findByIdForUpdate(2)).thenReturn(Optional.of(kelompok2));
        when(pesertaKelasRepository.existsByMahasiswa_NpmAndKelas_IdKelas(NPM, 101)).thenReturn(true);
        when(anggotaKelompokRepository.existsByMahasiswa_NpmAndKelas_IdKelas(NPM, 101)).thenReturn(false);
        when(anggotaKelompokRepository.countByKelompok_IdKelompok(2)).thenReturn(1L);

        MahasiswaException fullError = assertThrows(MahasiswaException.class, () -> service.gabungKelompok(currentStudent, 2));
        assertEquals(HttpStatus.CONFLICT, fullError.getStatus());

        when(anggotaKelompokRepository.existsByMahasiswa_NpmAndKelas_IdKelas(NPM, 101)).thenReturn(true);
        MahasiswaException duplicateError = assertThrows(MahasiswaException.class, () -> service.gabungKelompok(currentStudent, 2));
        assertEquals(HttpStatus.CONFLICT, duplicateError.getStatus());
        verify(anggotaKelompokRepository, never()).saveAndFlush(any());
    }

    @Test
    void keluarKelompokDeletesMembershipWhenNotScheduled() {
        AnggotaKelompok membership = new AnggotaKelompok(55, currentStudent, kelompok3, kelasAlgoritma);
        when(kelompokRepository.findByIdForUpdate(3)).thenReturn(Optional.of(kelompok3));
        when(anggotaKelompokRepository.findByMahasiswa_NpmAndKelompok_IdKelompok(NPM, 3))
            .thenReturn(Optional.of(membership));
        when(presentasiRepository.existsByKelompok_IdKelompok(3)).thenReturn(false);
        when(kelasRepository.findByIdWithDetails(101)).thenReturn(Optional.of(kelasAlgoritma));
        when(pesertaKelasRepository.existsByMahasiswa_NpmAndKelas_IdKelas(NPM, 101)).thenReturn(true);
        when(kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(101)).thenReturn(List.of(kelompok3));
        when(anggotaKelompokRepository.findByKelompokIdsWithMahasiswa(List.of(3))).thenReturn(List.of());
        when(presentasiRepository.findByKelasIdWithDetails(101)).thenReturn(List.of());

        DaftarKelompokKelasResponseDTO updated = service.keluarKelompok(currentStudent, 3);

        verify(anggotaKelompokRepository).delete(membership);
        verify(anggotaKelompokRepository).flush();
        assertNull(updated.idKelompokTerdaftar());
        assertEquals("Tersedia", updated.daftarKelompok().get(0).statusLabel());
    }

    @Test
    void keluarKelompokRejectsWhenPresentationScheduled() {
        AnggotaKelompok membership = new AnggotaKelompok(55, currentStudent, kelompok1, kelasAlgoritma);
        when(kelompokRepository.findByIdForUpdate(1)).thenReturn(Optional.of(kelompok1));
        when(anggotaKelompokRepository.findByMahasiswa_NpmAndKelompok_IdKelompok(NPM, 1))
            .thenReturn(Optional.of(membership));
        when(presentasiRepository.existsByKelompok_IdKelompok(1)).thenReturn(true);

        MahasiswaException err = assertThrows(MahasiswaException.class, () -> service.keluarKelompok(currentStudent, 1));
        assertEquals(HttpStatus.CONFLICT, err.getStatus());
        verify(anggotaKelompokRepository, never()).delete(any());
    }
}
