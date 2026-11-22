package com.unpar.scheduler.service;

import com.unpar.scheduler.controller.JadwalMengawasController;
import com.unpar.scheduler.dto.jadwalmengawas.JadwalMengawasActionResponseDTO;
import com.unpar.scheduler.dto.jadwalmengawas.JadwalMengawasResponseDTO;
import com.unpar.scheduler.dto.jadwalmengawas.PilihMengawasRequestDTO;
import com.unpar.scheduler.dto.jadwalmengawas.PindahMengawasRequestDTO;
import com.unpar.scheduler.dto.jadwalmengawas.UjianMengawasItemDTO;
import com.unpar.scheduler.model.entity.Akun;
import com.unpar.scheduler.model.entity.Dosen;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kelompok;
import com.unpar.scheduler.model.entity.Kuliah;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.MataKuliahBuka;
import com.unpar.scheduler.model.entity.PengawasUjian;
import com.unpar.scheduler.model.entity.PengawasUjianId;
import com.unpar.scheduler.model.entity.Presentasi;
import com.unpar.scheduler.model.entity.Ujian;
import com.unpar.scheduler.repository.DosenRepository;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KuliahRepository;
import com.unpar.scheduler.repository.PengawasUjianRepository;
import com.unpar.scheduler.repository.PresentasiRepository;
import com.unpar.scheduler.repository.UjianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JadwalMengawasServiceTest {

    private static final String NIDN_BUDI = "1975031501";
    private static final String NIDN_WIDODO = "1978042002";
    private static final String NIDN_YUNI = "1982091203";
    private static final String SEMESTER = "26271";

    @Mock
    private DosenRepository dosenRepository;
    @Mock
    private UjianRepository ujianRepository;
    @Mock
    private PengawasUjianRepository pengawasUjianRepository;
    @Mock
    private KelasRepository kelasRepository;
    @Mock
    private KuliahRepository kuliahRepository;
    @Mock
    private PresentasiRepository presentasiRepository;

    private JadwalMengawasService service;
    private JadwalMengawasController controller;

    private Dosen dosenBudi;
    private Dosen dosenWidodo;
    private Dosen dosenYuni;

    private MataKuliah mkAsd;
    private MataKuliah mkJarkom;
    private MataKuliah mkPbo;
    private MataKuliah mkRpl;

    private MataKuliahBuka mkbAsd;
    private MataKuliahBuka mkbJarkom;
    private MataKuliahBuka mkbPbo;
    private MataKuliahBuka mkbRpl;

    private Kelas kelasAsd;
    private Kelas kelasJarkom;

    private Ujian ujianAsdUts;
    private Ujian ujianAsdLabUas;
    private Ujian ujianJarkomUts;
    private Ujian ujianPboUtsConflict;
    private Ujian ujianRplUasFree;

    @BeforeEach
    void setUp() {
        service = new JadwalMengawasService(
            dosenRepository,
            ujianRepository,
            pengawasUjianRepository,
            kelasRepository,
            kuliahRepository,
            presentasiRepository
        );
        controller = new JadwalMengawasController(service);

        dosenBudi = new Dosen(NIDN_BUDI, "Dr. Budi Santoso", new Akun());
        dosenWidodo = new Dosen(NIDN_WIDODO, "Ir. Widodo Pratama, M.T.", new Akun());
        dosenYuni = new Dosen(NIDN_YUNI, "Dr. Yuni Kartika, S.T., M.Kom.", new Akun());

        mkAsd = new MataKuliah("IF2110", "Algoritma dan Struktur Data", (short) 4);
        mkJarkom = new MataKuliah("IF3130", "Jaringan Komputer", (short) 3);
        mkPbo = new MataKuliah("IF2210", "Pemrograman Berorientasi Objek", (short) 4);
        mkRpl = new MataKuliah("IF3250", "Rekayasa Perangkat Lunak", (short) 3);

        mkbAsd = new MataKuliahBuka(1, mkAsd, SEMESTER, dosenBudi);
        mkbJarkom = new MataKuliahBuka(2, mkJarkom, SEMESTER, dosenBudi);
        mkbPbo = new MataKuliahBuka(3, mkPbo, SEMESTER, dosenWidodo);
        mkbRpl = new MataKuliahBuka(4, mkRpl, SEMESTER, dosenYuni);

        kelasAsd = new Kelas(101, mkbAsd, "A", dosenBudi);
        kelasJarkom = new Kelas(102, mkbJarkom, "B", dosenBudi);

        // Monday 2026-10-19 08:00 - 10:00 (proctored by Budi)
        ujianAsdUts = new Ujian(
            10, mkbAsd, "UTS", LocalDate.of(2026, 10, 19),
            LocalTime.of(8, 0), LocalTime.of(10, 0), "Ruang 9012"
        );

        // Tuesday 2026-10-20 08:00 - 10:00 (overlaps with Budi's Tuesday 08:00-10:00 teaching schedule)
        ujianPboUtsConflict = new Ujian(
            20, mkbPbo, "UTS", LocalDate.of(2026, 10, 20),
            LocalTime.of(8, 0), LocalTime.of(10, 0), "Lab Komputer 2"
        );

        // Wednesday 2026-10-21 08:00 - 10:00 (proctored by Widodo for swap testing)
        ujianJarkomUts = new Ujian(
            30, mkbJarkom, "UTS", LocalDate.of(2026, 10, 21),
            LocalTime.of(8, 0), LocalTime.of(10, 0), "Ruang 9013"
        );

        // Thursday 2026-12-10 13:00 - 15:00 (Lab exam with 2 proctors: Widodo & Yuni)
        ujianAsdLabUas = new Ujian(
            40, mkbAsd, "UAS", LocalDate.of(2026, 12, 10),
            LocalTime.of(13, 0), LocalTime.of(15, 0), "Lab Komputer 1"
        );

        // Friday 2026-12-11 08:00 - 10:00 (0 proctors, no conflict)
        ujianRplUasFree = new Ujian(
            50, mkbRpl, "UAS", LocalDate.of(2026, 12, 11),
            LocalTime.of(8, 0), LocalTime.of(10, 0), "Ruang 9122"
        );
    }

    @Test
    void listSemesterExamsIdentifiesMultiProctorsAndConflictFlags() {
        when(kelasRepository.findLatestSemesterByLecturer(NIDN_BUDI)).thenReturn(Optional.of(SEMESTER));
        when(ujianRepository.findBySemesterWithDetails(SEMESTER)).thenReturn(List.of(
            ujianAsdUts, ujianPboUtsConflict, ujianJarkomUts, ujianAsdLabUas, ujianRplUasFree
        ));
        when(pengawasUjianRepository.findBySemesterWithDetails(SEMESTER)).thenReturn(List.of(
            new PengawasUjian(ujianAsdUts, dosenBudi),
            new PengawasUjian(ujianJarkomUts, dosenWidodo),
            new PengawasUjian(ujianAsdLabUas, dosenWidodo),
            new PengawasUjian(ujianAsdLabUas, dosenYuni)
        ));
        when(kelasRepository.findBySemester(SEMESTER)).thenReturn(List.of(kelasAsd, kelasJarkom));

        // Budi teaches ASD on Tuesday (hari=2) 08:00 - 10:00 -> conflicts with ujianPboUtsConflict
        Kuliah kuliahSelasa = new Kuliah(
            1, kelasAsd, dosenBudi, (short) 2,
            LocalTime.of(8, 0), LocalTime.of(10, 0), "Lab Komputer 1", "KULIAH"
        );
        when(kuliahRepository.findByDosenIdsWithDetails(List.of(NIDN_BUDI))).thenReturn(List.of(kuliahSelasa));
        when(presentasiRepository.findBySemesterWithDetails(SEMESTER)).thenReturn(Collections.emptyList());
        when(dosenRepository.findAll()).thenReturn(List.of(dosenBudi, dosenWidodo, dosenYuni));

        JadwalMengawasResponseDTO response = service.getJadwalMengawas(NIDN_BUDI);

        assertEquals(SEMESTER, response.semesterCode());
        assertEquals("Semester Ganjil 2026/2027", response.semesterLabel());
        assertEquals(1, response.myProctoringList().size());
        assertEquals(10, response.myProctoringList().get(0).idUjian());
        assertEquals(5, response.semesterExams().size());
        assertEquals(3, response.availableLecturers().size());

        // Exam 20 (Tuesday 08:00-10:00) must flag conflict against Budi's teaching schedule
        UjianMengawasItemDTO conflictExam = response.semesterExams().stream()
            .filter(e -> e.idUjian().equals(20))
            .findFirst()
            .orElseThrow();
        assertTrue(conflictExam.hasConflict());
        assertEquals(1, conflictExam.conflictDetails().size());
        assertTrue(conflictExam.conflictDetails().get(0).contains("IF2110"));

        // Exam 40 (Lab UAS) must list 2 existing proctors and allow additional proctor without conflict
        UjianMengawasItemDTO labExam = response.semesterExams().stream()
            .filter(e -> e.idUjian().equals(40))
            .findFirst()
            .orElseThrow();
        assertEquals(2, labExam.jumlahPengawas());
        assertFalse(labExam.isAssignedToMe());
        assertFalse(labExam.hasConflict());

        // Exam 50 (RPL UAS) has 0 proctors and no conflict
        UjianMengawasItemDTO unassignedExam = response.semesterExams().stream()
            .filter(e -> e.idUjian().equals(50))
            .findFirst()
            .orElseThrow();
        assertEquals(0, unassignedExam.jumlahPengawas());
        assertFalse(unassignedExam.hasConflict());
    }

    @Test
    void assignExamWithNoConflictSucceedsEvenWhenOtherProctorsAlreadyAssigned() {
        when(ujianRepository.findByIdWithDetails(40)).thenReturn(Optional.of(ujianAsdLabUas));
        when(dosenRepository.findByNomorIndukDosen(NIDN_BUDI)).thenReturn(Optional.of(dosenBudi));
        when(pengawasUjianRepository.existsById(new PengawasUjianId(40, NIDN_BUDI))).thenReturn(false);
        when(kuliahRepository.findByDosenIdsWithDetails(List.of(NIDN_BUDI))).thenReturn(Collections.emptyList());
        when(pengawasUjianRepository.findByTanggalAndDosenIdsWithDetails(ujianAsdLabUas.getTanggal(), List.of(NIDN_BUDI)))
            .thenReturn(Collections.emptyList());
        when(presentasiRepository.findByTanggalWithDetails(ujianAsdLabUas.getTanggal()))
            .thenReturn(Collections.emptyList());

        ResponseEntity<JadwalMengawasActionResponseDTO> httpRes = controller.pilihJadwalMengawas(
            NIDN_BUDI, new PilihMengawasRequestDTO(40, false)
        );

        assertEquals(HttpStatus.OK, httpRes.getStatusCode());
        assertNotNull(httpRes.getBody());
        assertTrue(httpRes.getBody().success());
        assertFalse(httpRes.getBody().conflict());
        assertEquals("Jadwal Berhasil Disimpan", httpRes.getBody().message());

        ArgumentCaptor<PengawasUjian> captor = ArgumentCaptor.forClass(PengawasUjian.class);
        verify(pengawasUjianRepository).save(captor.capture());
        assertEquals(40, captor.getValue().getId().getIdUjian());
        assertEquals(NIDN_BUDI, captor.getValue().getId().getNomorIndukDosen());
    }

    @Test
    void assignExamReturnsConflictWarningWhenOverrideFalseAndSucceedsWhenOverrideTrue() {
        Kuliah kuliahSelasa = new Kuliah(
            1, kelasAsd, dosenBudi, (short) 2,
            LocalTime.of(8, 0), LocalTime.of(10, 0), "Lab Komputer 1", "KULIAH"
        );
        Kelompok kel1 = new Kelompok(11, kelasAsd, (short) 1, (short) 4, "Topik Pohon");
        Presentasi presSelasa = new Presentasi(
            99, kel1, LocalDate.of(2026, 10, 20),
            LocalTime.of(9, 0), LocalTime.of(9, 30), "Lab Komputer 1", false, null
        );

        when(ujianRepository.findByIdWithDetails(20)).thenReturn(Optional.of(ujianPboUtsConflict));
        when(dosenRepository.findByNomorIndukDosen(NIDN_BUDI)).thenReturn(Optional.of(dosenBudi));
        when(pengawasUjianRepository.existsById(new PengawasUjianId(20, NIDN_BUDI))).thenReturn(false);
        when(kuliahRepository.findByDosenIdsWithDetails(List.of(NIDN_BUDI))).thenReturn(List.of(kuliahSelasa));
        when(pengawasUjianRepository.findByTanggalAndDosenIdsWithDetails(ujianPboUtsConflict.getTanggal(), List.of(NIDN_BUDI)))
            .thenReturn(Collections.emptyList());
        when(presentasiRepository.findByTanggalWithDetails(ujianPboUtsConflict.getTanggal()))
            .thenReturn(List.of(presSelasa));

        // 1. override = false -> returns HTTP 409 Conflict with conflict details and does NOT save
        ResponseEntity<JadwalMengawasActionResponseDTO> conflictRes = controller.pilihJadwalMengawas(
            NIDN_BUDI, new PilihMengawasRequestDTO(20, false)
        );

        assertEquals(HttpStatus.CONFLICT, conflictRes.getStatusCode());
        assertNotNull(conflictRes.getBody());
        assertFalse(conflictRes.getBody().success());
        assertTrue(conflictRes.getBody().conflict());
        assertEquals("Jadwal mengawas bentrok dengan jadwal Anda.", conflictRes.getBody().message());
        assertEquals(2, conflictRes.getBody().conflicts().size());
        verify(pengawasUjianRepository, never()).save(any());

        // 2. override = true -> saves record in pengawas_ujian and returns HTTP 200 OK
        ResponseEntity<JadwalMengawasActionResponseDTO> overrideRes = controller.pilihJadwalMengawas(
            NIDN_BUDI, new PilihMengawasRequestDTO(20, true)
        );

        assertEquals(HttpStatus.OK, overrideRes.getStatusCode());
        assertNotNull(overrideRes.getBody());
        assertTrue(overrideRes.getBody().success());
        assertFalse(overrideRes.getBody().conflict());
        assertEquals("Jadwal Berhasil Disimpan", overrideRes.getBody().message());
        verify(pengawasUjianRepository, times(1)).save(any(PengawasUjian.class));
    }

    @Test
    void cancelAssignedExamRemovesRecordAndRejectsIfUnassigned() {
        PengawasUjianId id = new PengawasUjianId(10, NIDN_BUDI);
        when(pengawasUjianRepository.existsById(id)).thenReturn(true);

        JadwalMengawasActionResponseDTO result = service.batalkanJadwalMengawas(NIDN_BUDI, 10);

        assertTrue(result.success());
        assertEquals("Jadwal mengawas berhasil dibatalkan.", result.message());
        verify(pengawasUjianRepository).deleteById(id);

        when(pengawasUjianRepository.existsById(new PengawasUjianId(50, NIDN_BUDI))).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.batalkanJadwalMengawas(NIDN_BUDI, 50));
    }

    @Test
    void moveExamWithOptionalReplacementLecturerAssignsBothCleanly() {
        PengawasUjianId sourceBudiId = new PengawasUjianId(10, NIDN_BUDI);

        when(ujianRepository.findByIdWithDetails(10)).thenReturn(Optional.of(ujianAsdUts));
        when(ujianRepository.findByIdWithDetails(50)).thenReturn(Optional.of(ujianRplUasFree));
        when(dosenRepository.findByNomorIndukDosen(NIDN_BUDI)).thenReturn(Optional.of(dosenBudi));
        when(dosenRepository.findByNomorIndukDosen(NIDN_YUNI)).thenReturn(Optional.of(dosenYuni));

        when(pengawasUjianRepository.existsById(sourceBudiId)).thenReturn(true);
        when(pengawasUjianRepository.existsById(new PengawasUjianId(50, NIDN_BUDI))).thenReturn(false);
        when(pengawasUjianRepository.existsById(new PengawasUjianId(10, NIDN_YUNI))).thenReturn(false);

        when(kuliahRepository.findByDosenIdsWithDetails(List.of(NIDN_BUDI))).thenReturn(Collections.emptyList());
        when(kuliahRepository.findByDosenIdsWithDetails(List.of(NIDN_YUNI))).thenReturn(Collections.emptyList());

        JadwalMengawasActionResponseDTO res = service.pindahJadwalMengawas(
            NIDN_BUDI,
            new PindahMengawasRequestDTO(10, 50, "MOVE", null, NIDN_YUNI, false)
        );

        assertTrue(res.success());
        assertFalse(res.conflict());
        assertEquals("Perpindahan jadwal mengawas berhasil disimpan.", res.message());

        verify(pengawasUjianRepository).deleteById(sourceBudiId);
        ArgumentCaptor<PengawasUjian> saveCaptor = ArgumentCaptor.forClass(PengawasUjian.class);
        verify(pengawasUjianRepository, times(2)).save(saveCaptor.capture());

        List<PengawasUjian> saved = saveCaptor.getAllValues();
        assertEquals(50, saved.get(0).getId().getIdUjian());
        assertEquals(NIDN_BUDI, saved.get(0).getId().getNomorIndukDosen());
        assertEquals(10, saved.get(1).getId().getIdUjian());
        assertEquals(NIDN_YUNI, saved.get(1).getId().getNomorIndukDosen());
    }

    @Test
    void swapProctoringAssignmentsBetweenTwoLecturersExcludesVacatedSlotsAndSwapsAtomically() {
        // Suppose sourceUjian (id=10) and targetUjian (id=30) are even at the same date/time in different rooms
        Ujian parallelTarget = new Ujian(
            30, mkbJarkom, "UTS", LocalDate.of(2026, 10, 19),
            LocalTime.of(8, 0), LocalTime.of(10, 0), "Ruang 9013"
        );
        PengawasUjian existingBudi = new PengawasUjian(ujianAsdUts, dosenBudi);
        PengawasUjian existingWidodo = new PengawasUjian(parallelTarget, dosenWidodo);

        when(ujianRepository.findByIdWithDetails(10)).thenReturn(Optional.of(ujianAsdUts));
        when(ujianRepository.findByIdWithDetails(30)).thenReturn(Optional.of(parallelTarget));
        when(dosenRepository.findByNomorIndukDosen(NIDN_BUDI)).thenReturn(Optional.of(dosenBudi));
        when(dosenRepository.findByNomorIndukDosen(NIDN_WIDODO)).thenReturn(Optional.of(dosenWidodo));

        when(pengawasUjianRepository.existsById(new PengawasUjianId(10, NIDN_BUDI))).thenReturn(true);
        when(pengawasUjianRepository.existsById(new PengawasUjianId(30, NIDN_BUDI))).thenReturn(false);

        when(pengawasUjianRepository.existsById(new PengawasUjianId(30, NIDN_WIDODO))).thenReturn(true);
        when(pengawasUjianRepository.existsById(new PengawasUjianId(10, NIDN_WIDODO))).thenReturn(false);

        // Both lecturers are proctoring on 2026-10-19 08:00-10:00 (the exact slots they are vacating!)
        when(pengawasUjianRepository.findByTanggalAndDosenIdsWithDetails(LocalDate.of(2026, 10, 19), List.of(NIDN_BUDI)))
            .thenReturn(List.of(existingBudi));
        when(pengawasUjianRepository.findByTanggalAndDosenIdsWithDetails(LocalDate.of(2026, 10, 19), List.of(NIDN_WIDODO)))
            .thenReturn(List.of(existingWidodo));
        when(kuliahRepository.findByDosenIdsWithDetails(List.of(NIDN_BUDI))).thenReturn(Collections.emptyList());
        when(kuliahRepository.findByDosenIdsWithDetails(List.of(NIDN_WIDODO))).thenReturn(Collections.emptyList());

        JadwalMengawasActionResponseDTO res = service.pindahJadwalMengawas(
            NIDN_BUDI,
            new PindahMengawasRequestDTO(10, 30, "SWAP", NIDN_WIDODO, null, false)
        );

        assertTrue(res.success());
        assertFalse(res.conflict());
        assertEquals("Perpindahan jadwal mengawas berhasil disimpan.", res.message());

        verify(pengawasUjianRepository).deleteById(new PengawasUjianId(10, NIDN_BUDI));
        verify(pengawasUjianRepository).deleteById(new PengawasUjianId(30, NIDN_WIDODO));

        ArgumentCaptor<PengawasUjian> saveCaptor = ArgumentCaptor.forClass(PengawasUjian.class);
        verify(pengawasUjianRepository, times(2)).save(saveCaptor.capture());

        List<PengawasUjian> saved = saveCaptor.getAllValues();
        assertEquals(30, saved.get(0).getId().getIdUjian());
        assertEquals(NIDN_BUDI, saved.get(0).getId().getNomorIndukDosen());
        assertEquals(10, saved.get(1).getId().getIdUjian());
        assertEquals(NIDN_WIDODO, saved.get(1).getId().getNomorIndukDosen());
    }
}
