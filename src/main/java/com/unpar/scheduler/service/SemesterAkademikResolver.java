package com.unpar.scheduler.service;

import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Menentukan semester akademik aktif dari PostgreSQL (prioritas peserta_kelas mahasiswa, lalu mata_kuliah_buka).
@Component
public class SemesterAkademikResolver {

    static final String SEMESTER_BELUM_TERSEDIA = "Semester Belum Tersedia";

    private final KelasRepository kelasRepository;
    private final PesertaKelasRepository pesertaKelasRepository;

    public SemesterAkademikResolver(
        KelasRepository kelasRepository,
        PesertaKelasRepository pesertaKelasRepository
    ) {
        this.kelasRepository = kelasRepository;
        this.pesertaKelasRepository = pesertaKelasRepository;
    }

    public Optional<String> resolveActiveSemesterForMahasiswa(String npm) {
        if (npm != null && !npm.isBlank()) {
            Optional<String> enrolled = normalize(pesertaKelasRepository.findLatestSemesterByMahasiswaNpm(npm.trim()));
            if (enrolled.isPresent()) {
                return enrolled;
            }
        }
        return normalize(kelasRepository.findLatestSemesterOverall());
    }

    // Mendukung kode YY1YY2T (mis. 26271 -> 2026/2027) dan YYYYT (mis. 20261 -> 2026/2027).
    public static String formatSemesterLabel(String kodeSemester) {
        if (kodeSemester == null || kodeSemester.isBlank()) {
            return SEMESTER_BELUM_TERSEDIA;
        }
        String trimmed = kodeSemester.trim();
        if (trimmed.length() == 5 && trimmed.chars().allMatch(Character::isDigit)) {
            String termName = switch (trimmed.charAt(4)) {
                case '1' -> "Semester Ganjil";
                case '2' -> "Semester Genap";
                case '3' -> "Semester Pendek";
                default -> null;
            };
            if (termName != null) {
                int[] years = parseAcademicYears(trimmed);
                return termName + " " + years[0] + "/" + years[1];
            }
        }
        return trimmed;
    }

    private static int[] parseAcademicYears(String code) {
        int firstTwo = Integer.parseInt(code.substring(0, 2));
        int secondTwo = Integer.parseInt(code.substring(2, 4));
        if (secondTwo == (firstTwo + 1) % 100) {
            int startYear = 2000 + firstTwo;
            return new int[]{startYear, startYear + 1};
        }
        if (code.startsWith("20") || code.startsWith("19")) {
            int startYear = Integer.parseInt(code.substring(0, 4));
            return new int[]{startYear, startYear + 1};
        }
        return new int[]{2000 + firstTwo, 2000 + secondTwo};
    }

    private static Optional<String> normalize(Optional<String> value) {
        return value.map(String::trim).filter(code -> !code.isEmpty());
    }
}
