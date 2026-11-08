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
            return buildFallbackSummary(targetNidn);
        }

        Dosen dosen = dosenOpt.get();
        String activeSemester = DEFAULT_FALLBACK_SEMESTER;
        List<Kelas> classes = kelasRepository.findClassesByLecturerAndSemester(dosen.getNomorIndukDosen(), activeSemester);

        if (classes.isEmpty()) {
            return buildFallbackSummary(dosen.getNomorIndukDosen(), dosen.getNama(), dosen.getAkun() != null ? dosen.getAkun().getEmail() : DEFAULT_FALLBACK_EMAIL);
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
            activeSemester,
            items
        );
    }

    private DosenDashboardSummaryDTO buildFallbackSummary(String nidn) {
        return buildFallbackSummary(nidn, DEFAULT_FALLBACK_NAME, DEFAULT_FALLBACK_EMAIL);
    }

    private DosenDashboardSummaryDTO buildFallbackSummary(String nidn, String name, String email) {
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
            DEFAULT_FALLBACK_SEMESTER,
            fallbackItems
        );
    }
}
