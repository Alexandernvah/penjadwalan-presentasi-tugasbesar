package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.dashboard.DosenDashboardSummaryDTO;
import com.unpar.scheduler.dto.dashboard.MataKuliahAjarItemDTO;
import com.unpar.scheduler.model.entity.Dosen;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kuliah;
import com.unpar.scheduler.repository.DosenRepository;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KuliahRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DosenDashboardService {

    private static final String DEFAULT_FALLBACK_NIDN = "1975031501";
    private static final String DEFAULT_FALLBACK_NAME = "Dr. Budi Santoso";
    private static final String DEFAULT_FALLBACK_EMAIL = "budi.santoso@unpar.ac.id";
    private static final String DEFAULT_FALLBACK_SEMESTER = "25261";

    private final DosenRepository dosenRepository;
    private final KelasRepository kelasRepository;
    private final KuliahRepository kuliahRepository;

    public DosenDashboardService(
        DosenRepository dosenRepository,
        KelasRepository kelasRepository,
        KuliahRepository kuliahRepository
    ) {
        this.dosenRepository = dosenRepository;
        this.kelasRepository = kelasRepository;
        this.kuliahRepository = kuliahRepository;
    }

    public DosenDashboardSummaryDTO getDashboardSummary(String nidn) {
        String targetNidn = (nidn != null && !nidn.trim().isEmpty()) ? nidn.trim() : DEFAULT_FALLBACK_NIDN;

        Optional<Dosen> dosenOpt = dosenRepository.findByNomorIndukDosen(targetNidn);
        if (dosenOpt.isEmpty()) {
            String fallbackSemester = resolveActiveSemesterCode(null);
            return buildFallbackSummary(targetNidn, DEFAULT_FALLBACK_NAME, DEFAULT_FALLBACK_EMAIL, fallbackSemester);
        }

        Dosen dosen = dosenOpt.get();
        String activeSemester = resolveActiveSemesterCode(dosen.getNomorIndukDosen());
        List<Kelas> classes = kelasRepository.findClassesByLecturerAndSemester(dosen.getNomorIndukDosen(), activeSemester);

        if (classes.isEmpty()) {
            return buildFallbackSummary(
                dosen.getNomorIndukDosen(),
                dosen.getNama(),
                dosen.getAkun() != null ? dosen.getAkun().getEmail() : DEFAULT_FALLBACK_EMAIL,
                activeSemester
            );
        }

        List<MataKuliahAjarItemDTO> items = new ArrayList<>();
        for (Kelas kelas : classes) {
            List<Kuliah> kuliahList = kuliahRepository.findByKelas_IdKelasAndDosenPengajar_NomorIndukDosen(
                kelas.getIdKelas(),
                dosen.getNomorIndukDosen()
            );

            if (!kuliahList.isEmpty()) {
                for (Kuliah kuliah : kuliahList) {
                    items.add(new MataKuliahAjarItemDTO(
                        kelas.getIdKelas(),
                        kelas.getMataKuliahBuka().getMatakuliah().getIdMatkul(),
                        kelas.getMataKuliahBuka().getMatakuliah().getNama(),
                        kelas.getKodeKelas(),
                        kelas.getMataKuliahBuka().getMatakuliah().getSks(),
                        kuliah.getRuangan(),
                        kuliah.getHari(),
                        kuliah.getJamMulai(),
                        kuliah.getJamSelesai()
                    ));
                }
            } else {
                items.add(new MataKuliahAjarItemDTO(
                    kelas.getIdKelas(),
                    kelas.getMataKuliahBuka().getMatakuliah().getIdMatkul(),
                    kelas.getMataKuliahBuka().getMatakuliah().getNama(),
                    kelas.getKodeKelas(),
                    kelas.getMataKuliahBuka().getMatakuliah().getSks(),
                    "-",
                    (short) 1,
                    LocalTime.of(8, 0),
                    LocalTime.of(10, 0)
                ));
            }
        }

        String lecturerEmail = (dosen.getAkun() != null && dosen.getAkun().getEmail() != null)
            ? dosen.getAkun().getEmail()
            : DEFAULT_FALLBACK_EMAIL;

        return new DosenDashboardSummaryDTO(
            dosen.getNomorIndukDosen(),
            dosen.getNama(),
            lecturerEmail,
            formatSemesterLabel(activeSemester),
            items
        );
    }

    private String resolveActiveSemesterCode(String nidn) {
        if (nidn != null && !nidn.isBlank()) {
            Optional<String> lecturerSemester = kelasRepository.findLatestSemesterByLecturer(nidn);
            if (lecturerSemester.isPresent() && !lecturerSemester.get().isBlank()) {
                return lecturerSemester.get().trim();
            }
        }
        Optional<String> overallSemester = kelasRepository.findLatestSemesterOverall();
        if (overallSemester.isPresent() && !overallSemester.get().isBlank()) {
            return overallSemester.get().trim();
        }
        return DEFAULT_FALLBACK_SEMESTER;
    }

    public static String formatSemesterLabel(String kodeSemester) {
        if (kodeSemester == null || kodeSemester.isBlank()) {
            return formatSemesterLabel(DEFAULT_FALLBACK_SEMESTER);
        }
        String trimmed = kodeSemester.trim();
        if (trimmed.length() == 5 && trimmed.chars().allMatch(Character::isDigit)) {
            int startYear = 2000 + Integer.parseInt(trimmed.substring(0, 2));
            int endYear = 2000 + Integer.parseInt(trimmed.substring(2, 4));
            char termDigit = trimmed.charAt(4);
            String termName = switch (termDigit) {
                case '1' -> "Semester Ganjil";
                case '2' -> "Semester Genap";
                case '3' -> "Semester Pendek";
                default -> null;
            };
            if (termName != null) {
                return termName + " " + startYear + "/" + endYear;
            }
        }
        return trimmed;
    }

    private DosenDashboardSummaryDTO buildFallbackSummary(String nidn, String name, String email, String semesterCode) {
        List<MataKuliahAjarItemDTO> fallbackItems = List.of(
            new MataKuliahAjarItemDTO(
                101,
                "IF2110",
                "Algoritma dan Struktur Data",
                "A",
                (short) 4,
                "Lab Komputer 1",
                (short) 1,
                LocalTime.of(8, 0),
                LocalTime.of(10, 30)
            ),
            new MataKuliahAjarItemDTO(
                102,
                "IF3130",
                "Jaringan Komputer",
                "B",
                (short) 3,
                "Ruang 9122",
                (short) 3,
                LocalTime.of(13, 0),
                LocalTime.of(15, 30)
            )
        );

        return new DosenDashboardSummaryDTO(
            (nidn != null && !nidn.trim().isEmpty()) ? nidn : DEFAULT_FALLBACK_NIDN,
            name,
            email,
            formatSemesterLabel(semesterCode),
            fallbackItems
        );
    }
}
