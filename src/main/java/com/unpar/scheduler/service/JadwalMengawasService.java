package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.jadwalmengawas.DosenRingkasDTO;
import com.unpar.scheduler.dto.jadwalmengawas.JadwalMengawasActionResponseDTO;
import com.unpar.scheduler.dto.jadwalmengawas.JadwalMengawasResponseDTO;
import com.unpar.scheduler.dto.jadwalmengawas.PilihMengawasRequestDTO;
import com.unpar.scheduler.dto.jadwalmengawas.PindahMengawasRequestDTO;
import com.unpar.scheduler.dto.jadwalmengawas.UjianMengawasItemDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class JadwalMengawasService {

    private static final String DEFAULT_NIDN = "1975031501";
    private static final String DEFAULT_SEMESTER = "26271";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] NAMA_HARI = {
        "", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"
    };

    private final DosenRepository dosenRepository;
    private final UjianRepository ujianRepository;
    private final PengawasUjianRepository pengawasUjianRepository;
    private final KelasRepository kelasRepository;
    private final KuliahRepository kuliahRepository;
    private final PresentasiRepository presentasiRepository;

    public JadwalMengawasService(
        DosenRepository dosenRepository,
        UjianRepository ujianRepository,
        PengawasUjianRepository pengawasUjianRepository,
        KelasRepository kelasRepository,
        KuliahRepository kuliahRepository,
        PresentasiRepository presentasiRepository
    ) {
        this.dosenRepository = dosenRepository;
        this.ujianRepository = ujianRepository;
        this.pengawasUjianRepository = pengawasUjianRepository;
        this.kelasRepository = kelasRepository;
        this.kuliahRepository = kuliahRepository;
        this.presentasiRepository = presentasiRepository;
    }

    @Transactional(readOnly = true)
    public JadwalMengawasResponseDTO getJadwalMengawas(String nidn) {
        String targetNidn = normalizeNidn(nidn);
        String activeSemester = resolveActiveSemesterCode(targetNidn);
        String semesterLabel = DosenDashboardService.formatSemesterLabel(activeSemester);

        List<Ujian> exams = new ArrayList<>(ujianRepository.findBySemesterWithDetails(activeSemester));
        exams.sort(
            Comparator.comparing(Ujian::getTanggal, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Ujian::getJamMulai, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Ujian::getIdUjian, Comparator.nullsLast(Comparator.naturalOrder()))
        );

        List<PengawasUjian> allProctors = pengawasUjianRepository.findBySemesterWithDetails(activeSemester);
        Map<Integer, Map<String, DosenRingkasDTO>> proctorsByExam = new HashMap<>();
        List<PengawasUjian> lecturerProctoring = new ArrayList<>();

        for (PengawasUjian pu : allProctors) {
            if (pu.getUjian() == null || pu.getDosen() == null) {
                continue;
            }
            Integer idUjian = pu.getUjian().getIdUjian();
            String proctorNidn = safeTrim(pu.getDosen().getNomorIndukDosen());
            proctorsByExam
                .computeIfAbsent(idUjian, k -> new LinkedHashMap<>())
                .put(proctorNidn, new DosenRingkasDTO(proctorNidn, pu.getDosen().getNama()));

            if (targetNidn.equals(proctorNidn)) {
                lecturerProctoring.add(pu);
            }
        }

        Map<Integer, String> namaKelasByMatkulBukaId = buildNamaKelasMap(activeSemester);
        List<Kuliah> lecturerKuliah = kuliahRepository.findByDosenIdsWithDetails(List.of(targetNidn));
        Set<Integer> taughtKelasIds = extractTaughtKelasIds(lecturerKuliah);
        List<Presentasi> semesterPresentations = presentasiRepository.findBySemesterWithDetails(activeSemester);

        List<UjianMengawasItemDTO> semesterExams = new ArrayList<>();
        List<UjianMengawasItemDTO> myProctoringList = new ArrayList<>();

        for (Ujian ujian : exams) {
            MataKuliahBuka mkb = ujian.getMataKuliahBuka();
            MataKuliah mk = mkb != null ? mkb.getMatakuliah() : null;
            String kodeMatkul = mk != null ? mk.getIdMatkul() : "-";
            String namaMatkul = mk != null ? mk.getNama() : "-";
            String namaKelas = (mkb != null && namaKelasByMatkulBukaId.containsKey(mkb.getIdMatkulBuka()))
                ? namaKelasByMatkulBukaId.get(mkb.getIdMatkulBuka())
                : "Kelas A";

            List<DosenRingkasDTO> pengawasList = new ArrayList<>(
                proctorsByExam.getOrDefault(ujian.getIdUjian(), Collections.emptyMap()).values()
            );
            boolean isAssignedToMe = pengawasList.stream().anyMatch(p -> targetNidn.equals(p.nidn()));

            List<String> conflictDetails = evaluateConflictsInMemory(
                targetNidn,
                ujian,
                null,
                lecturerKuliah,
                taughtKelasIds,
                lecturerProctoring,
                semesterPresentations
            );

            String hari = ujian.getTanggal() != null
                ? namaHari(ujian.getTanggal().getDayOfWeek().getValue())
                : "-";

            UjianMengawasItemDTO item = new UjianMengawasItemDTO(
                ujian.getIdUjian(),
                kodeMatkul,
                namaMatkul,
                namaKelas,
                ujian.getJenisUjian(),
                ujian.getTanggal() != null ? ujian.getTanggal().toString() : "",
                hari,
                formatTime(ujian.getJamMulai()),
                formatTime(ujian.getJamSelesai()),
                ujian.getRuangan(),
                pengawasList,
                pengawasList.size(),
                isAssignedToMe,
                !conflictDetails.isEmpty(),
                conflictDetails
            );

            semesterExams.add(item);
            if (isAssignedToMe) {
                myProctoringList.add(item);
            }
        }

        List<DosenRingkasDTO> availableLecturers = dosenRepository.findAll().stream()
            .filter(d -> d.getNomorIndukDosen() != null)
            .map(d -> new DosenRingkasDTO(safeTrim(d.getNomorIndukDosen()), d.getNama()))
            .sorted(Comparator.comparing(DosenRingkasDTO::nama, Comparator.nullsLast(String::compareToIgnoreCase)))
            .toList();

        return new JadwalMengawasResponseDTO(
            semesterLabel,
            activeSemester,
            myProctoringList,
            semesterExams,
            availableLecturers
        );
    }

    public JadwalMengawasActionResponseDTO pilihJadwalMengawas(String nidn, PilihMengawasRequestDTO request) {
        String targetNidn = normalizeNidn(nidn);
        if (request == null || request.idUjian() == null) {
            throw new IllegalArgumentException("ID ujian wajib diisi.");
        }

        Ujian ujian = resolveUjian(request.idUjian());
        Dosen dosen = resolveDosen(targetNidn, "Data dosen tidak ditemukan.");

        if (isDosenAssignedToUjian(ujian.getIdUjian(), targetNidn)) {
            throw new IllegalArgumentException("Anda sudah terdaftar sebagai pengawas pada ujian ini.");
        }

        List<String> conflicts = findConflictsForLecturer(targetNidn, ujian, null);
        boolean override = Boolean.TRUE.equals(request.override());

        if (!conflicts.isEmpty() && !override) {
            return new JadwalMengawasActionResponseDTO(
                false,
                true,
                "Jadwal mengawas bentrok dengan jadwal Anda.",
                conflicts
            );
        }

        pengawasUjianRepository.save(new PengawasUjian(ujian, dosen));
        return new JadwalMengawasActionResponseDTO(
            true,
            false,
            "Jadwal Berhasil Disimpan",
            Collections.emptyList()
        );
    }

    public JadwalMengawasActionResponseDTO batalkanJadwalMengawas(String nidn, Integer idUjian) {
        String targetNidn = normalizeNidn(nidn);
        if (idUjian == null) {
            throw new IllegalArgumentException("ID ujian wajib diisi.");
        }

        if (!isDosenAssignedToUjian(idUjian, targetNidn)) {
            throw new IllegalArgumentException("Anda belum terdaftar sebagai pengawas pada ujian ini.");
        }

        removeProctorAssignment(idUjian, targetNidn);
        return new JadwalMengawasActionResponseDTO(
            true,
            false,
            "Jadwal mengawas berhasil dibatalkan.",
            Collections.emptyList()
        );
    }

    public JadwalMengawasActionResponseDTO pindahJadwalMengawas(String nidn, PindahMengawasRequestDTO request) {
        String targetNidn = normalizeNidn(nidn);
        if (request == null || request.sourceUjianId() == null || request.targetUjianId() == null) {
            throw new IllegalArgumentException("Ujian asal dan ujian tujuan wajib dipilih.");
        }
        if (request.sourceUjianId().equals(request.targetUjianId())) {
            throw new IllegalArgumentException("Ujian asal dan ujian tujuan tidak boleh sama.");
        }

        Ujian sourceUjian = resolveUjian(request.sourceUjianId());
        Ujian targetUjian = resolveUjian(request.targetUjianId());
        Dosen currentDosen = resolveDosen(targetNidn, "Data dosen tidak ditemukan.");

        if (!isDosenAssignedToUjian(sourceUjian.getIdUjian(), targetNidn)) {
            throw new IllegalArgumentException("Anda tidak terdaftar sebagai pengawas pada ujian asal.");
        }
        if (isDosenAssignedToUjian(targetUjian.getIdUjian(), targetNidn)) {
            throw new IllegalArgumentException("Anda sudah terdaftar sebagai pengawas pada ujian tujuan.");
        }

        String mode = (request.mode() != null && !request.mode().isBlank())
            ? request.mode().trim().toUpperCase()
            : "MOVE";
        boolean override = Boolean.TRUE.equals(request.override());

        if ("MOVE".equals(mode)) {
            Dosen replacementDosen = null;
            String repNidn = null;
            if (request.replacementDosenId() != null && !request.replacementDosenId().isBlank()) {
                repNidn = request.replacementDosenId().trim();
                if (repNidn.equals(targetNidn)) {
                    throw new IllegalArgumentException("Dosen pengganti tidak boleh sama dengan dosen yang pindah.");
                }
                replacementDosen = resolveDosen(repNidn, "Dosen pengganti tidak ditemukan.");
                if (isDosenAssignedToUjian(sourceUjian.getIdUjian(), repNidn)) {
                    throw new IllegalArgumentException("Dosen pengganti sudah terdaftar sebagai pengawas pada ujian asal.");
                }
            }

            List<String> allConflicts = new ArrayList<>();
            List<String> myConflicts = findConflictsForLecturer(targetNidn, targetUjian, sourceUjian.getIdUjian());
            for (String c : myConflicts) {
                allConflicts.add(currentDosen.getNama() + ": " + c);
            }

            if (replacementDosen != null && repNidn != null) {
                List<String> repConflicts = findConflictsForLecturer(repNidn, sourceUjian, null);
                for (String c : repConflicts) {
                    allConflicts.add(replacementDosen.getNama() + ": " + c);
                }
            }

            if (!allConflicts.isEmpty() && !override) {
                return new JadwalMengawasActionResponseDTO(
                    false,
                    true,
                    "Terdapat bentrok jadwal pada perpindahan pengawas.",
                    allConflicts
                );
            }

            removeProctorAssignment(sourceUjian.getIdUjian(), targetNidn);
            pengawasUjianRepository.save(new PengawasUjian(targetUjian, currentDosen));
            if (replacementDosen != null) {
                pengawasUjianRepository.save(new PengawasUjian(sourceUjian, replacementDosen));
            }

            return new JadwalMengawasActionResponseDTO(
                true,
                false,
                "Perpindahan jadwal mengawas berhasil disimpan.",
                Collections.emptyList()
            );
        }

        if ("SWAP".equals(mode)) {
            if (request.targetDosenId() == null || request.targetDosenId().isBlank()) {
                throw new IllegalArgumentException("Dosen tujuan tukar jadwal wajib dipilih.");
            }
            String swapNidn = request.targetDosenId().trim();
            if (swapNidn.equals(targetNidn)) {
                throw new IllegalArgumentException("Tidak dapat menukar jadwal dengan diri sendiri.");
            }

            Dosen swapDosen = resolveDosen(swapNidn, "Dosen tujuan tukar jadwal tidak ditemukan.");
            if (!isDosenAssignedToUjian(targetUjian.getIdUjian(), swapNidn)) {
                throw new IllegalArgumentException("Dosen tujuan tidak terdaftar sebagai pengawas pada ujian tujuan.");
            }
            if (isDosenAssignedToUjian(sourceUjian.getIdUjian(), swapNidn)) {
                throw new IllegalArgumentException("Dosen tujuan sudah terdaftar sebagai pengawas pada ujian asal.");
            }

            List<String> allConflicts = new ArrayList<>();
            List<String> myConflicts = findConflictsForLecturer(targetNidn, targetUjian, sourceUjian.getIdUjian());
            for (String c : myConflicts) {
                allConflicts.add(currentDosen.getNama() + ": " + c);
            }

            List<String> swapConflicts = findConflictsForLecturer(swapNidn, sourceUjian, targetUjian.getIdUjian());
            for (String c : swapConflicts) {
                allConflicts.add(swapDosen.getNama() + ": " + c);
            }

            if (!allConflicts.isEmpty() && !override) {
                return new JadwalMengawasActionResponseDTO(
                    false,
                    true,
                    "Terdapat bentrok jadwal pada perpindahan pengawas.",
                    allConflicts
                );
            }

            removeProctorAssignment(sourceUjian.getIdUjian(), targetNidn);
            removeProctorAssignment(targetUjian.getIdUjian(), swapNidn);
            pengawasUjianRepository.save(new PengawasUjian(targetUjian, currentDosen));
            pengawasUjianRepository.save(new PengawasUjian(sourceUjian, swapDosen));

            return new JadwalMengawasActionResponseDTO(
                true,
                false,
                "Perpindahan jadwal mengawas berhasil disimpan.",
                Collections.emptyList()
            );
        }

        throw new IllegalArgumentException("Mode perpindahan tidak valid. Gunakan MOVE atau SWAP.");
    }

    private List<String> findConflictsForLecturer(String nidn, Ujian targetUjian, Integer excludedUjianId) {
        if (targetUjian == null || targetUjian.getTanggal() == null) {
            return Collections.emptyList();
        }
        LocalDate tanggal = targetUjian.getTanggal();
        short hari = (short) tanggal.getDayOfWeek().getValue();

        List<Kuliah> allDosenKuliah = kuliahRepository.findByDosenIdsWithDetails(List.of(nidn));
        List<Kuliah> dayKuliah = allDosenKuliah.isEmpty()
            ? kuliahRepository.findByHariAndDosenIdsWithDetails(hari, List.of(nidn))
            : allDosenKuliah;
        Set<Integer> taughtKelasIds = extractTaughtKelasIds(dayKuliah);

        List<PengawasUjian> proctoringOnDate = pengawasUjianRepository.findByTanggalAndDosenIdsWithDetails(
            tanggal, List.of(nidn)
        );
        if (proctoringOnDate.isEmpty() && targetUjian.getMataKuliahBuka() != null
            && targetUjian.getMataKuliahBuka().getSemesterAkademik() != null) {
            String sem = safeTrim(targetUjian.getMataKuliahBuka().getSemesterAkademik());
            for (PengawasUjian pu : pengawasUjianRepository.findByDosenAndSemesterWithDetails(nidn, sem)) {
                if (pu.getUjian() != null && tanggal.equals(pu.getUjian().getTanggal())) {
                    proctoringOnDate.add(pu);
                }
            }
        }

        List<Presentasi> presentasiOnDate = presentasiRepository.findByTanggalWithDetails(tanggal);
        if (presentasiOnDate.isEmpty() && targetUjian.getMataKuliahBuka() != null
            && targetUjian.getMataKuliahBuka().getSemesterAkademik() != null) {
            String sem = safeTrim(targetUjian.getMataKuliahBuka().getSemesterAkademik());
            for (Presentasi p : presentasiRepository.findBySemesterWithDetails(sem)) {
                if (tanggal.equals(p.getTanggal())) {
                    presentasiOnDate.add(p);
                }
            }
        }

        return evaluateConflictsInMemory(
            nidn,
            targetUjian,
            excludedUjianId,
            dayKuliah,
            taughtKelasIds,
            proctoringOnDate,
            presentasiOnDate
        );
    }

    private List<String> evaluateConflictsInMemory(
        String nidn,
        Ujian targetUjian,
        Integer excludedUjianId,
        List<Kuliah> kuliahList,
        Set<Integer> taughtKelasIds,
        List<PengawasUjian> proctoringList,
        List<Presentasi> presentasiList
    ) {
        if (targetUjian == null || targetUjian.getTanggal() == null) {
            return Collections.emptyList();
        }

        LocalDate tanggal = targetUjian.getTanggal();
        short hari = (short) tanggal.getDayOfWeek().getValue();
        LocalTime mulai = targetUjian.getJamMulai();
        LocalTime selesai = targetUjian.getJamSelesai();
        List<String> conflicts = new ArrayList<>();

        if (kuliahList != null) {
            for (Kuliah kul : kuliahList) {
                if (kul.getHari() == null || kul.getHari() != hari) {
                    continue;
                }
                if (kul.getDosenPengajar() != null
                    && !nidn.equals(safeTrim(kul.getDosenPengajar().getNomorIndukDosen()))) {
                    continue;
                }
                if (PenjadwalanService.isOverlapping(mulai, selesai, kul.getJamMulai(), kul.getJamSelesai())) {
                    Kelas k = kul.getKelas();
                    MataKuliah mk = (k != null && k.getMataKuliahBuka() != null)
                        ? k.getMataKuliahBuka().getMatakuliah()
                        : null;
                    String kodeMk = mk != null ? mk.getIdMatkul() : "-";
                    String namaMk = mk != null ? mk.getNama() : "Perkuliahan";
                    String kodeKelas = k != null ? k.getKodeKelas() : "-";
                    String desc = "Kuliah " + kodeMk + " - " + namaMk + " (Kelas " + kodeKelas + ") hari "
                        + namaHari(hari) + " (" + formatTime(kul.getJamMulai()) + " - "
                        + formatTime(kul.getJamSelesai()) + ") di " + kul.getRuangan();
                    if (!conflicts.contains(desc)) {
                        conflicts.add(desc);
                    }
                }
            }
        }

        if (proctoringList != null) {
            for (PengawasUjian pu : proctoringList) {
                Ujian other = pu.getUjian();
                if (other == null || other.getTanggal() == null || !tanggal.equals(other.getTanggal())) {
                    continue;
                }
                if (targetUjian.getIdUjian() != null && targetUjian.getIdUjian().equals(other.getIdUjian())) {
                    continue;
                }
                if (excludedUjianId != null && excludedUjianId.equals(other.getIdUjian())) {
                    continue;
                }
                if (pu.getDosen() != null && !nidn.equals(safeTrim(pu.getDosen().getNomorIndukDosen()))) {
                    continue;
                }
                if (PenjadwalanService.isOverlapping(mulai, selesai, other.getJamMulai(), other.getJamSelesai())) {
                    MataKuliah mk = (other.getMataKuliahBuka() != null)
                        ? other.getMataKuliahBuka().getMatakuliah()
                        : null;
                    String kodeMk = mk != null ? mk.getIdMatkul() : "-";
                    String namaMk = mk != null ? mk.getNama() : "Ujian";
                    String desc = "Mengawas " + other.getJenisUjian() + " " + kodeMk + " - " + namaMk
                        + " (" + other.getTanggal() + ", " + formatTime(other.getJamMulai()) + " - "
                        + formatTime(other.getJamSelesai()) + ") di " + other.getRuangan();
                    if (!conflicts.contains(desc)) {
                        conflicts.add(desc);
                    }
                }
            }
        }

        if (presentasiList != null) {
            for (Presentasi p : presentasiList) {
                if (p.getTanggal() == null || !tanggal.equals(p.getTanggal())) {
                    continue;
                }
                if (!isLecturerAssociatedWithPresentation(p, nidn, taughtKelasIds)) {
                    continue;
                }
                if (PenjadwalanService.isOverlapping(mulai, selesai, p.getJamMulai(), p.getJamSelesai())) {
                    Kelompok kel = p.getKelompok();
                    Kelas k = kel != null ? kel.getKelas() : null;
                    MataKuliah mk = (k != null && k.getMataKuliahBuka() != null)
                        ? k.getMataKuliahBuka().getMatakuliah()
                        : null;
                    String kodeMk = mk != null ? mk.getIdMatkul() : "-";
                    String namaMk = mk != null ? mk.getNama() : "Tugas Besar";
                    short noKel = (kel != null && kel.getNoKelompok() != null) ? kel.getNoKelompok() : 1;
                    String desc = "Presentasi Tugas Besar " + kodeMk + " - " + namaMk
                        + " (Kelompok " + noKel + ") (" + p.getTanggal() + ", "
                        + formatTime(p.getJamMulai()) + " - " + formatTime(p.getJamSelesai())
                        + ") di " + p.getRuangan();
                    if (!conflicts.contains(desc)) {
                        conflicts.add(desc);
                    }
                }
            }
        }

        return conflicts;
    }

    private boolean isLecturerAssociatedWithPresentation(Presentasi p, String nidn, Set<Integer> taughtKelasIds) {
        if (p == null || p.getKelompok() == null || p.getKelompok().getKelas() == null) {
            return false;
        }
        Kelas k = p.getKelompok().getKelas();
        if (k.getIdKelas() != null && taughtKelasIds != null && taughtKelasIds.contains(k.getIdKelas())) {
            return true;
        }
        if (k.getPenanggungJawab() != null
            && nidn.equals(safeTrim(k.getPenanggungJawab().getNomorIndukDosen()))) {
            return true;
        }
        return k.getMataKuliahBuka() != null
            && k.getMataKuliahBuka().getKoordinator() != null
            && nidn.equals(safeTrim(k.getMataKuliahBuka().getKoordinator().getNomorIndukDosen()));
    }

    private Set<Integer> extractTaughtKelasIds(List<Kuliah> kuliahList) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (kuliahList != null) {
            for (Kuliah k : kuliahList) {
                if (k.getKelas() != null && k.getKelas().getIdKelas() != null) {
                    ids.add(k.getKelas().getIdKelas());
                }
            }
        }
        return ids;
    }

    private Map<Integer, String> buildNamaKelasMap(String semester) {
        Map<Integer, Set<String>> codesByMkb = new HashMap<>();
        for (Kelas k : kelasRepository.findBySemester(semester)) {
            if (k.getMataKuliahBuka() != null && k.getMataKuliahBuka().getIdMatkulBuka() != null
                && k.getKodeKelas() != null && !k.getKodeKelas().isBlank()) {
                codesByMkb
                    .computeIfAbsent(k.getMataKuliahBuka().getIdMatkulBuka(), id -> new LinkedHashSet<>())
                    .add(k.getKodeKelas().trim());
            }
        }
        Map<Integer, String> result = new HashMap<>();
        codesByMkb.forEach((mkbId, codes) -> result.put(mkbId, "Kelas " + String.join(", ", codes)));
        return result;
    }

    private Ujian resolveUjian(Integer idUjian) {
        Optional<Ujian> withDetails = ujianRepository.findByIdWithDetails(idUjian);
        if (withDetails.isPresent()) {
            return withDetails.get();
        }
        return ujianRepository.findById(idUjian)
            .orElseThrow(() -> new IllegalArgumentException("Data ujian tidak ditemukan: " + idUjian));
    }

    private Dosen resolveDosen(String nidn, String notFoundMessage) {
        return dosenRepository.findByNomorIndukDosen(nidn)
            .orElseThrow(() -> new IllegalArgumentException(notFoundMessage));
    }

    private boolean isDosenAssignedToUjian(Integer idUjian, String nidn) {
        return pengawasUjianRepository.existsById(new PengawasUjianId(idUjian, nidn));
    }

    private void removeProctorAssignment(Integer idUjian, String nidn) {
        pengawasUjianRepository.deleteById(new PengawasUjianId(idUjian, nidn));
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
        return DEFAULT_SEMESTER;
    }

    private static String normalizeNidn(String nidn) {
        return (nidn != null && !nidn.isBlank()) ? nidn.trim() : DEFAULT_NIDN;
    }

    private static String safeTrim(String value) {
        return value != null ? value.trim() : "";
    }

    private static String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FMT) : "";
    }

    private static String namaHari(int hari) {
        return hari >= 1 && hari <= 7 ? NAMA_HARI[hari] : "-";
    }
}
