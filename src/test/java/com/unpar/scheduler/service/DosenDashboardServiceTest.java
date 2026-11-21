package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.dashboard.DosenDashboardSummaryDTO;
import com.unpar.scheduler.model.entity.Akun;
import com.unpar.scheduler.model.entity.Dosen;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kuliah;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.MataKuliahBuka;
import com.unpar.scheduler.repository.DosenRepository;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KuliahRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DosenDashboardServiceTest {

    private static final String NIDN = "1975031501";

    @Mock
    private DosenRepository dosenRepository;
    @Mock
    private KelasRepository kelasRepository;
    @Mock
    private KuliahRepository kuliahRepository;

    private DosenDashboardService service;

    @BeforeEach
    void setUp() {
        service = new DosenDashboardService(dosenRepository, kelasRepository, kuliahRepository);
    }

    @Test
    void formatSemesterLabelConvertsCodesAndPreservesText() {
        assertEquals("Semester Ganjil 2025/2026", DosenDashboardService.formatSemesterLabel("25261"));
        assertEquals("Semester Ganjil 2026/2027", DosenDashboardService.formatSemesterLabel("26271"));
        assertEquals("Semester Genap 2025/2026", DosenDashboardService.formatSemesterLabel("25262"));
        assertEquals("Semester Genap 2026/2027", DosenDashboardService.formatSemesterLabel("26272"));
        assertEquals("Semester Pendek 2025/2026", DosenDashboardService.formatSemesterLabel("25263"));
        assertEquals("Semester Ganjil 2026/2027", DosenDashboardService.formatSemesterLabel("Semester Ganjil 2026/2027"));
        assertEquals("Semester Ganjil 2025/2026", DosenDashboardService.formatSemesterLabel(null));
    }

    @Test
    void dashboardResolvesLatestSemesterFromLecturerClasses() {
        Akun akun = new Akun();
        akun.setEmail("budi.santoso@unpar.ac.id");
        Dosen dosen = new Dosen(NIDN, "Dr. Budi Santoso", akun);
        MataKuliah mk = new MataKuliah("IF3130", "Jaringan Komputer", (short) 3);
        MataKuliahBuka mkb = new MataKuliahBuka(10, mk, "26271", dosen);
        Kelas kelas = new Kelas(102, mkb, "B", dosen);
        Kuliah kuliah = new Kuliah(1, kelas, dosen, (short) 1, LocalTime.of(13, 0), LocalTime.of(15, 0), "Ruang 9013", "KULIAH");

        when(dosenRepository.findByNomorIndukDosen(NIDN)).thenReturn(Optional.of(dosen));
        when(kelasRepository.findLatestSemesterByLecturer(NIDN)).thenReturn(Optional.of("26271"));
        when(kelasRepository.findClassesByLecturerAndSemester(NIDN, "26271")).thenReturn(List.of(kelas));
        when(kuliahRepository.findByKelas_IdKelasAndDosenPengajar_NomorIndukDosen(102, NIDN)).thenReturn(List.of(kuliah));

        DosenDashboardSummaryDTO summary = service.getDashboardSummary(NIDN);

        assertEquals("Semester Ganjil 2026/2027", summary.semesterAktif());
        assertEquals(1, summary.mataKuliahAjar().size());
    }

    @Test
    void fallbackSummaryUsesLatestOverallSemesterWhenPresent() {
        when(dosenRepository.findByNomorIndukDosen(NIDN)).thenReturn(Optional.empty());
        when(kelasRepository.findLatestSemesterOverall()).thenReturn(Optional.of("25262"));

        DosenDashboardSummaryDTO summary = service.getDashboardSummary(NIDN);

        assertEquals("Semester Genap 2025/2026", summary.semesterAktif());
    }
}
