package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.dashboard.DosenDashboardSummaryDTO;
import com.unpar.scheduler.dto.dashboard.MataKuliahAjarItemDTO;
import com.unpar.scheduler.dto.kelompok.AnggotaItemDTO;
import com.unpar.scheduler.dto.penjadwalan.AnggotaPenjadwalanDTO;
import com.unpar.scheduler.dto.penjadwalan.BarisMatriksMingguanDTO;
import com.unpar.scheduler.dto.penjadwalan.CekBentrokRequestDTO;
import com.unpar.scheduler.dto.penjadwalan.CekBentrokResponseDTO;
import com.unpar.scheduler.dto.penjadwalan.HariMingguanDTO;
import com.unpar.scheduler.dto.penjadwalan.KelasPenjadwalanDTO;
import com.unpar.scheduler.dto.penjadwalan.KelompokPenjadwalanDTO;
import com.unpar.scheduler.dto.penjadwalan.KonflikDetailDTO;
import com.unpar.scheduler.dto.penjadwalan.MatriksMingguanResponseDTO;
import com.unpar.scheduler.dto.penjadwalan.MatriksPenjadwalanResponseDTO;
import com.unpar.scheduler.dto.penjadwalan.PresentasiTerjadwalDTO;
import com.unpar.scheduler.dto.penjadwalan.RuanganOptionDTO;
import com.unpar.scheduler.dto.penjadwalan.SelMatriksMingguanDTO;
import com.unpar.scheduler.dto.penjadwalan.SimpanPresentasiRequestDTO;
import com.unpar.scheduler.dto.penjadwalan.SlotMatriksDTO;
import com.unpar.scheduler.model.entity.AnggotaKelompok;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kelompok;
import com.unpar.scheduler.model.entity.Kuliah;
import com.unpar.scheduler.model.entity.PengawasUjian;
import com.unpar.scheduler.model.entity.PesertaKelas;
import com.unpar.scheduler.model.entity.Presentasi;
import com.unpar.scheduler.model.entity.Ujian;
import com.unpar.scheduler.repository.AnggotaKelompokRepository;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KelompokRepository;
import com.unpar.scheduler.repository.KuliahRepository;
import com.unpar.scheduler.repository.PengawasUjianRepository;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import com.unpar.scheduler.repository.PresentasiRepository;
import com.unpar.scheduler.repository.UjianRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class PenjadwalanService {

    private static final String DEFAULT_NIDN = "1975031501";
    private static final String DEFAULT_DOSEN_NAME = "Dr. Budi Santoso";
    private static final String DEFAULT_DOSEN_EMAIL = "budi.santoso@unpar.ac.id";
    private static final String DEFAULT_RUANGAN = "Ruang 9122";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DOT_TIME_FMT = DateTimeFormatter.ofPattern("HH.mm");

    private static final LocalDate DEFAULT_REGULER_MONDAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate DEFAULT_UTS_MONDAY = LocalDate.of(2026, 10, 19);
    private static final LocalDate DEFAULT_UAS_MONDAY = LocalDate.of(2026, 12, 7);

    private final DosenDashboardService dosenDashboardService;
    private final KelasRepository kelasRepository;
    private final KelompokRepository kelompokRepository;
    private final AnggotaKelompokRepository anggotaKelompokRepository;
    private final PesertaKelasRepository pesertaKelasRepository;
    private final KuliahRepository kuliahRepository;
    private final UjianRepository ujianRepository;
    private final PengawasUjianRepository pengawasUjianRepository;
    private final PresentasiRepository presentasiRepository;

    // Fallback in-memory presentation store when database tables are unseeded
    private final Map<Integer, PresentasiTerjadwalDTO> fallbackPresentasiById = new ConcurrentHashMap<>();
    private final AtomicInteger fallbackIdSequence = new AtomicInteger(900);

    public PenjadwalanService(
        DosenDashboardService dosenDashboardService,
        KelasRepository kelasRepository,
        KelompokRepository kelompokRepository,
        AnggotaKelompokRepository anggotaKelompokRepository,
        PesertaKelasRepository pesertaKelasRepository,
        KuliahRepository kuliahRepository,
        UjianRepository ujianRepository,
        PengawasUjianRepository pengawasUjianRepository,
        PresentasiRepository presentasiRepository
    ) {
        this.dosenDashboardService = dosenDashboardService;
        this.kelasRepository = kelasRepository;
        this.kelompokRepository = kelompokRepository;
        this.anggotaKelompokRepository = anggotaKelompokRepository;
        this.pesertaKelasRepository = pesertaKelasRepository;
        this.kuliahRepository = kuliahRepository;
        this.ujianRepository = ujianRepository;
        this.pengawasUjianRepository = pengawasUjianRepository;
        this.presentasiRepository = presentasiRepository;
    }

    /**
     * Predikat irisan waktu 4 dimensi: mulaiA < selesaiB && mulaiB < selesaiA
     */
    public static boolean isOverlapping(LocalTime mulaiA, LocalTime selesaiA, LocalTime mulaiB, LocalTime selesaiB) {
        if (mulaiA == null || selesaiA == null || mulaiB == null || selesaiB == null) {
            return false;
        }
        return mulaiA.isBefore(selesaiB) && mulaiB.isBefore(selesaiA);
    }

    @Transactional(readOnly = true)
    public List<KelasPenjadwalanDTO> getDaftarKelasDosen(String dosenId) {
        DosenDashboardSummaryDTO summary = dosenDashboardService.getDashboardSummary(dosenId);
        String namaDosen = summary.namaDosen() != null ? summary.namaDosen() : DEFAULT_DOSEN_NAME;
        String emailDosen = summary.email() != null ? summary.email() : DEFAULT_DOSEN_EMAIL;

        Map<Integer, KelasPenjadwalanDTO> uniqueClasses = new LinkedHashMap<>();
        if (summary.mataKuliahAjar() != null) {
            for (MataKuliahAjarItemDTO item : summary.mataKuliahAjar()) {
                if (item.idKelas() != null && !uniqueClasses.containsKey(item.idKelas())) {
                    String ruangan = (item.ruangan() != null && !item.ruangan().isBlank() && !"-".equals(item.ruangan()))
                        ? item.ruangan()
                        : DEFAULT_RUANGAN;
                    uniqueClasses.put(item.idKelas(), new KelasPenjadwalanDTO(
                        item.idKelas(),
                        item.idMatkul(),
                        item.namaMatkul(),
                        item.kodeKelas(),
                        item.sks(),
                        ruangan,
                        namaDosen,
                        emailDosen
                    ));
                }
            }
        }

        if (uniqueClasses.isEmpty()) {
            uniqueClasses.put(102, new KelasPenjadwalanDTO(
                102, "IF3130", "Jaringan Komputer", "B", (short) 3, "Ruang 9122", namaDosen, emailDosen
            ));
            uniqueClasses.put(101, new KelasPenjadwalanDTO(
                101, "IF2110", "Algoritma dan Struktur Data", "A", (short) 4, "Lab Komputer 1", namaDosen, emailDosen
            ));
        }

        return new ArrayList<>(uniqueClasses.values());
    }

    @Transactional(readOnly = true)
    public List<RuanganOptionDTO> getDaftarRuangan() {
        Map<String, RuanganOptionDTO> rooms = new LinkedHashMap<>();

        addStandardRoom(rooms, "Ruang 9122", "Ruang 9122 (Gedung 9 Lantai 1)", "Gedung 9");
        addStandardRoom(rooms, "Ruang 9121", "Ruang 9121 (Gedung 9 Lantai 1)", "Gedung 9");
        addStandardRoom(rooms, "Lab Komputer 1", "Lab Komputer 1 (Gedung 9 Lantai 2)", "Gedung 9");
        addStandardRoom(rooms, "Lab Komputer 2", "Lab Komputer 2 (Gedung 9 Lantai 2)", "Gedung 9");
        addStandardRoom(rooms, "Lab Jaringan 9015", "Lab Jaringan 9015 (Gedung 9 Dasar)", "Gedung 9");

        try {
            for (String r : kuliahRepository.findDistinctRuangan()) {
                addDynamicRoom(rooms, r);
            }
            for (String r : ujianRepository.findDistinctRuangan()) {
                addDynamicRoom(rooms, r);
            }
            for (String r : presentasiRepository.findDistinctRuangan()) {
                addDynamicRoom(rooms, r);
            }
        } catch (Exception ignored) {
            // Keep standard rooms if DB query fails
        }

        return new ArrayList<>(rooms.values());
    }

    private void addStandardRoom(Map<String, RuanganOptionDTO> map, String kode, String label, String gedung) {
        map.put(kode.toLowerCase(), new RuanganOptionDTO(kode, label, gedung));
    }

    private void addDynamicRoom(Map<String, RuanganOptionDTO> map, String rawRoom) {
        if (rawRoom == null || rawRoom.isBlank() || "-".equals(rawRoom.trim())) {
            return;
        }
        String clean = rawRoom.trim();
        String key = clean.toLowerCase();
        if (!map.containsKey(key)) {
            map.put(key, new RuanganOptionDTO(clean, clean, "Gedung Perkuliahan"));
        }
    }

    @Transactional(readOnly = true)
    public List<KelompokPenjadwalanDTO> getKelompokByKelas(Integer idKelas) {
        if (idKelas == null) {
            return Collections.emptyList();
        }

        List<Kelompok> kelompokEntities = kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(idKelas);
        if (kelompokEntities.isEmpty()) {
            return getFallbackKelompokByKelas(idKelas);
        }

        // Batch fetch all members and presentations for this class (zero N+1 queries)
        List<AnggotaKelompok> allAnggota = anggotaKelompokRepository.findByKelasIdWithMahasiswaAndKelompok(idKelas);
        Map<Integer, List<AnggotaItemDTO>> rawAnggotaByKelompok = new HashMap<>();
        for (AnggotaKelompok ak : allAnggota) {
            if (ak.getKelompok() != null && ak.getMahasiswa() != null) {
                rawAnggotaByKelompok
                    .computeIfAbsent(ak.getKelompok().getIdKelompok(), k -> new ArrayList<>())
                    .add(new AnggotaItemDTO(ak.getMahasiswa().getNpm(), ak.getMahasiswa().getNama()));
            }
        }

        List<Presentasi> allPresentasi = presentasiRepository.findByKelasIdWithDetails(idKelas);
        Map<Integer, Presentasi> presentasiByKelompok = new HashMap<>();
        for (Presentasi p : allPresentasi) {
            if (p.getKelompok() != null) {
                presentasiByKelompok.put(p.getKelompok().getIdKelompok(), p);
            }
        }

        List<KelompokPenjadwalanDTO> result = new ArrayList<>();
        for (Kelompok kel : kelompokEntities) {
            Integer idKel = kel.getIdKelompok();
            int nomor = kel.getNoKelompok() != null ? kel.getNoKelompok().intValue() : 1;
            int kapasitas = kel.getKapasitas() != null ? kel.getKapasitas().intValue() : 4;
            String topik = normalizeTopik(kel.getTopik(), nomor);

            List<AnggotaPenjadwalanDTO> anggota = toAnggotaPenjadwalanList(
                rawAnggotaByKelompok.getOrDefault(idKel, Collections.emptyList())
            );
            Presentasi pres = presentasiByKelompok.get(idKel);

            if (pres != null) {
                int durasi = computeDurationMinutes(pres.getJamMulai(), pres.getJamSelesai(), 30);
                result.add(new KelompokPenjadwalanDTO(
                    idKel,
                    nomor,
                    nomor,
                    topik,
                    kapasitas,
                    durasi,
                    true,
                    pres.getIdPresentasi(),
                    resolveHariIndonesia(pres.getTanggal().getDayOfWeek().getValue()),
                    pres.getTanggal().toString(),
                    pres.getJamMulai().format(TIME_FMT),
                    pres.getJamSelesai().format(TIME_FMT),
                    pres.getRuangan(),
                    pres.getCatatan(),
                    anggota
                ));
            } else {
                result.add(new KelompokPenjadwalanDTO(
                    idKel,
                    nomor,
                    nomor,
                    topik,
                    kapasitas,
                    30,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    anggota
                ));
            }
        }

        return result;
    }

    private String normalizeTopik(String rawTopik, int nomorKelompok) {
        if (rawTopik == null || rawTopik.isBlank()) {
            return "Belum ada topik tugas besar";
        }
        String trimmed = rawTopik.trim();
        if ("-".equals(trimmed)
            || "belum ditentukan".equalsIgnoreCase(trimmed)
            || ("Kelompok " + nomorKelompok).equalsIgnoreCase(trimmed)
            || trimmed.matches("(?i)^Kelompok\\s+\\d+$")) {
            return "Belum ada topik tugas besar";
        }
        return trimmed;
    }

    private List<AnggotaPenjadwalanDTO> toAnggotaPenjadwalanList(List<AnggotaItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<AnggotaPenjadwalanDTO> list = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            AnggotaItemDTO m = items.get(i);
            list.add(new AnggotaPenjadwalanDTO(m.npm(), m.nama(), i == 0));
        }
        return list;
    }

    /**
     * Weekly 5-Day (Senin - Jumat, 07:00 - 18:00) Combined Schedule Matrix
     * Supports REGULER, UTS, and UAS modes with batch queries (zero N+1 loops).
     */
    @Transactional(readOnly = true)
    public MatriksMingguanResponseDTO getMatriksMingguan(
        Integer idKelas,
        Integer idKelompok,
        String mode,
        LocalDate tanggalMulai,
        String dosenId
    ) {
        Integer targetKelas = idKelas != null ? idKelas : 102;
        String normalizedMode = normalizeMode(mode);
        LocalDate monday = resolveWeekMonday(normalizedMode, tanggalMulai);
        LocalDate friday = monday.plusDays(4);

        List<HariMingguanDTO> hariList = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            LocalDate d = monday.plusDays(i);
            String namaHari = resolveHariIndonesia(i + 1);
            String labelSingkat = formatDayMonthShortIndo(d);
            hariList.add(new HariMingguanDTO(
                i + 1,
                namaHari,
                d.toString(),
                labelSingkat,
                namaHari + ", " + labelSingkat
            ));
        }

        String rentangLabel = formatDateFullIndo(monday) + " - " + formatDateFullIndo(friday);
        String prevMonday = monday.minusWeeks(1).toString();
        String nextMonday = monday.plusWeeks(1).toString();

        Optional<Kelas> dbKelasOpt = kelasRepository.findByIdWithDetails(targetKelas);
        List<Kelompok> dbKelompokList = kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(targetKelas);

        if (dbKelasOpt.isEmpty() && dbKelompokList.isEmpty()) {
            return buildFallbackMatriksMingguan(
                targetKelas, idKelompok, normalizedMode, monday, friday, rentangLabel, prevMonday, nextMonday, hariList
            );
        }

        Kelas kelas = dbKelasOpt.orElse(null);
        Kelompok selectedKelompok = null;
        if (!dbKelompokList.isEmpty()) {
            if (idKelompok != null) {
                for (Kelompok k : dbKelompokList) {
                    if (idKelompok.equals(k.getIdKelompok())) {
                        selectedKelompok = k;
                        break;
                    }
                }
            }
            if (selectedKelompok == null) {
                selectedKelompok = dbKelompokList.get(0);
            }
        }

        Integer activeKelompokId = selectedKelompok != null
            ? selectedKelompok.getIdKelompok()
            : (idKelompok != null ? idKelompok : 1);
        int activeNoKelompok = (selectedKelompok != null && selectedKelompok.getNoKelompok() != null)
            ? selectedKelompok.getNoKelompok().intValue()
            : 1;

        // Batch load all weekly schedule data (Dosen, Mahasiswa, Kelas, Presentasi) in fixed queries
        WeeklyBatchContext weeklyCtx = loadWeeklyBatchContext(
            targetKelas, kelas, activeKelompokId, monday, friday, dosenId
        );

        int durasiMenit = 30;
        Presentasi existingKelompokPres = weeklyCtx.presentasiByKelompokId().get(activeKelompokId);
        if (existingKelompokPres != null) {
            durasiMenit = computeDurationMinutes(existingKelompokPres.getJamMulai(), existingKelompokPres.getJamSelesai(), 30);
        }

        String idMatkul = (kelas != null && kelas.getMataKuliahBuka() != null && kelas.getMataKuliahBuka().getMatakuliah() != null)
            ? kelas.getMataKuliahBuka().getMatakuliah().getIdMatkul()
            : "IF3130";
        String namaMatkul = (kelas != null && kelas.getMataKuliahBuka() != null && kelas.getMataKuliahBuka().getMatakuliah() != null)
            ? kelas.getMataKuliahBuka().getMatakuliah().getNama()
            : "Jaringan Komputer";
        String kodeKelas = kelas != null ? kelas.getKodeKelas() : "B";
        String namaKelasFull = idMatkul + " - " + namaMatkul + " (Kelas " + kodeKelas + ")";
        String namaSingkat = abbreviateCourseName(namaMatkul);
        String ruanganDefault = resolveDefaultRuanganKelas(weeklyCtx.kuliahKelasIni());

        // Evaluate 5 days x 11 hours (07:00 - 18:00)
        RawCell[][] grid = new RawCell[11][5];
        for (int dIdx = 0; dIdx < 5; dIdx++) {
            HariMingguanDTO hariDto = hariList.get(dIdx);
            LocalDate date = LocalDate.parse(hariDto.tanggal());
            short hariKe = (short) hariDto.hariKe();

            for (int hIdx = 0; hIdx < 11; hIdx++) {
                int hour = 7 + hIdx;
                LocalTime slotStart = LocalTime.of(hour, 0);
                LocalTime slotEnd = LocalTime.of(hour + 1, 0);
                grid[hIdx][dIdx] = evaluateWeeklyCellState(
                    weeklyCtx,
                    targetKelas,
                    kelas,
                    activeKelompokId,
                    namaSingkat,
                    normalizedMode,
                    hariKe,
                    date,
                    slotStart,
                    slotEnd
                );
            }
        }

        List<BarisMatriksMingguanDTO> barisList = buildMergedRowsWithRowSpan(grid, hariList);

        return new MatriksMingguanResponseDTO(
            targetKelas,
            namaKelasFull,
            idMatkul,
            namaSingkat,
            ruanganDefault,
            activeKelompokId,
            activeNoKelompok,
            durasiMenit,
            normalizedMode,
            monday.toString(),
            friday.toString(),
            rentangLabel,
            prevMonday,
            nextMonday,
            hariList,
            barisList
        );
    }

    private WeeklyBatchContext loadWeeklyBatchContext(
        Integer idKelas,
        Kelas kelas,
        Integer idKelompok,
        LocalDate monday,
        LocalDate friday,
        String requestedDosenId
    ) {
        Set<String> dosenIds = new LinkedHashSet<>();
        if (requestedDosenId != null && !requestedDosenId.isBlank()) {
            dosenIds.add(requestedDosenId.trim());
        }
        if (kelas != null) {
            if (kelas.getPenanggungJawab() != null && kelas.getPenanggungJawab().getNomorIndukDosen() != null) {
                dosenIds.add(kelas.getPenanggungJawab().getNomorIndukDosen());
            }
            if (kelas.getMataKuliahBuka() != null
                && kelas.getMataKuliahBuka().getKoordinator() != null
                && kelas.getMataKuliahBuka().getKoordinator().getNomorIndukDosen() != null) {
                dosenIds.add(kelas.getMataKuliahBuka().getKoordinator().getNomorIndukDosen());
            }
        }

        List<Kuliah> kuliahKelasIni = kuliahRepository.findByKelas_IdKelas(idKelas);
        for (Kuliah kul : kuliahKelasIni) {
            if (kul.getDosenPengajar() != null && kul.getDosenPengajar().getNomorIndukDosen() != null) {
                dosenIds.add(kul.getDosenPengajar().getNomorIndukDosen());
            }
        }
        if (dosenIds.isEmpty()) {
            dosenIds.add(DEFAULT_NIDN);
        }

        // Batch queries for Lecturer
        List<Kuliah> kuliahDosenAllDays = kuliahRepository.findByDosenIdsWithDetails(dosenIds);
        List<PengawasUjian> pengawasDosenWeek = pengawasUjianRepository.findByTanggalBetweenAndDosenIdsWithDetails(
            monday, friday, dosenIds
        );
        List<Presentasi> presentasiWeekAll = presentasiRepository.findByTanggalBetweenWithDetails(monday, friday);

        // Batch queries for Students in the selected group
        List<AnggotaKelompok> anggotaKelas = anggotaKelompokRepository.findByKelasIdWithMahasiswaAndKelompok(idKelas);
        Set<String> groupNpms = new LinkedHashSet<>();
        for (AnggotaKelompok ak : anggotaKelas) {
            if (ak.getKelompok() != null && ak.getMahasiswa() != null) {
                if (idKelompok == null || idKelompok.equals(ak.getKelompok().getIdKelompok())) {
                    groupNpms.add(ak.getMahasiswa().getNpm());
                }
            }
        }

        List<PesertaKelas> pesertaMahasiswa = groupNpms.isEmpty()
            ? Collections.emptyList()
            : pesertaKelasRepository.findByMahasiswaNpmInWithDetails(groupNpms);

        Set<Integer> studentKelasIds = new LinkedHashSet<>();
        Set<Integer> studentMatkulBukaIds = new LinkedHashSet<>();
        if (kelas != null && kelas.getMataKuliahBuka() != null) {
            studentMatkulBukaIds.add(kelas.getMataKuliahBuka().getIdMatkulBuka());
        }
        for (PesertaKelas pk : pesertaMahasiswa) {
            if (pk.getKelas() != null) {
                studentKelasIds.add(pk.getKelas().getIdKelas());
                if (pk.getKelas().getMataKuliahBuka() != null) {
                    studentMatkulBukaIds.add(pk.getKelas().getMataKuliahBuka().getIdMatkulBuka());
                }
            }
        }

        List<Kuliah> kuliahMahasiswaAllDays = studentKelasIds.isEmpty()
            ? Collections.emptyList()
            : kuliahRepository.findByKelasIdsWithDetails(studentKelasIds);

        List<Ujian> ujianMahasiswaWeek = studentMatkulBukaIds.isEmpty()
            ? Collections.emptyList()
            : ujianRepository.findByTanggalBetweenAndMatkulBukaIds(monday, friday, studentMatkulBukaIds);

        List<AnggotaKelompok> anggotaSemuaKelasMahasiswa = groupNpms.isEmpty()
            ? Collections.emptyList()
            : anggotaKelompokRepository.findByMahasiswaNpmInWithDetails(groupNpms);

        List<Presentasi> presentasiKelasIni = presentasiRepository.findByKelasIdWithDetails(idKelas);
        Map<Integer, Presentasi> presentasiByKelompokId = new HashMap<>();
        for (Presentasi p : presentasiKelasIni) {
            if (p.getKelompok() != null) {
                presentasiByKelompokId.put(p.getKelompok().getIdKelompok(), p);
            }
        }

        return new WeeklyBatchContext(
            dosenIds,
            kuliahKelasIni,
            kuliahDosenAllDays,
            pengawasDosenWeek,
            presentasiWeekAll,
            pesertaMahasiswa,
            kuliahMahasiswaAllDays,
            ujianMahasiswaWeek,
            anggotaSemuaKelasMahasiswa,
            presentasiByKelompokId
        );
    }

    private RawCell evaluateWeeklyCellState(
        WeeklyBatchContext ctx,
        Integer idKelas,
        Kelas kelas,
        Integer idKelompok,
        String namaSingkatMatkul,
        String mode,
        short hariKe,
        LocalDate date,
        LocalTime slotStart,
        LocalTime slotEnd
    ) {
        Integer classMatkulBukaId = (kelas != null && kelas.getMataKuliahBuka() != null)
            ? kelas.getMataKuliahBuka().getIdMatkulBuka()
            : null;

        // 1. Check Existing Presentations (TERJADWAL / BENTROK_DOSEN)
        List<String> bookedIntervals = new ArrayList<>();
        Presentasi ownPresentation = null;
        Presentasi otherPresentation = null;

        for (Presentasi p : ctx.presentasiWeekAll()) {
            if (p.getKelompok() == null || !date.equals(p.getTanggal())) {
                continue;
            }
            if (!isOverlapping(slotStart, slotEnd, p.getJamMulai(), p.getJamSelesai())) {
                continue;
            }
            String pRange = p.getJamMulai().format(TIME_FMT) + " - " + p.getJamSelesai().format(TIME_FMT);
            if (!bookedIntervals.contains(pRange)) {
                bookedIntervals.add(pRange);
            }

            if (idKelompok != null && idKelompok.equals(p.getKelompok().getIdKelompok())) {
                ownPresentation = p;
            } else {
                Kelas pKelas = p.getKelompok().getKelas();
                boolean sameLecturer = false;
                if (pKelas != null) {
                    if (idKelas.equals(pKelas.getIdKelas())) {
                        sameLecturer = true;
                    } else if (pKelas.getPenanggungJawab() != null
                        && ctx.dosenIds().contains(pKelas.getPenanggungJawab().getNomorIndukDosen())) {
                        sameLecturer = true;
                    } else if (pKelas.getMataKuliahBuka() != null
                        && pKelas.getMataKuliahBuka().getKoordinator() != null
                        && ctx.dosenIds().contains(pKelas.getMataKuliahBuka().getKoordinator().getNomorIndukDosen())) {
                        sameLecturer = true;
                    }
                }
                if (sameLecturer) {
                    otherPresentation = p;
                }
            }
        }

        if (ownPresentation != null) {
            int noKel = ownPresentation.getKelompok().getNoKelompok() != null
                ? ownPresentation.getKelompok().getNoKelompok().intValue()
                : 1;
            String timeDotRange = ownPresentation.getJamMulai().format(DOT_TIME_FMT)
                + " - " + ownPresentation.getJamSelesai().format(DOT_TIME_FMT);
            String matkulName = resolveNamaMatkul(ownPresentation, kelas);
            return new RawCell(
                "TERJADWAL",
                "Terjadwal",
                "Kelompok " + noKel + "\n" + matkulName + "\n" + timeDotRange,
                "",
                "",
                ownPresentation.getIdPresentasi(),
                bookedIntervals
            );
        }

        if (otherPresentation != null) {
            int noKel = otherPresentation.getKelompok().getNoKelompok() != null
                ? otherPresentation.getKelompok().getNoKelompok().intValue()
                : 1;
            String timeDotRange = otherPresentation.getJamMulai().format(DOT_TIME_FMT)
                + " - " + otherPresentation.getJamSelesai().format(DOT_TIME_FMT);
            String matkulName = resolveNamaMatkul(otherPresentation, kelas);
            return new RawCell(
                "BENTROK_DOSEN",
                "Dosen Sibuk",
                "Menguji Kelompok " + noKel + "\n" + matkulName + "\n" + timeDotRange,
                "",
                "",
                otherPresentation.getIdPresentasi(),
                bookedIntervals
            );
        }

        // 2. Check Lecturer Teaching / Exam Conflict (BENTROK_DOSEN)
        if ("REGULER".equals(mode)) {
            for (Kuliah kul : ctx.kuliahDosenAllDays()) {
                if (kul.getHari() != null && kul.getHari() == hariKe
                    && !idKelas.equals(kul.getKelas().getIdKelas())
                    && isOverlapping(slotStart, slotEnd, kul.getJamMulai(), kul.getJamSelesai())) {
                    return new RawCell("BENTROK_DOSEN", "Dosen Sibuk", "Mengajar Kelas Lain", "", "", null, bookedIntervals);
                }
            }
        } else {
            for (PengawasUjian pu : ctx.pengawasDosenWeek()) {
                Ujian u = pu.getUjian();
                if (u != null
                    && date.equals(u.getTanggal())
                    && mode.equalsIgnoreCase(u.getJenisUjian())
                    && (classMatkulBukaId == null || !classMatkulBukaId.equals(u.getMataKuliahBuka().getIdMatkulBuka()))
                    && isOverlapping(slotStart, slotEnd, u.getJamMulai(), u.getJamSelesai())) {
                    String mkShort = abbreviateCourseName(u.getMataKuliahBuka().getMatakuliah().getNama());
                    return new RawCell("BENTROK_DOSEN", "Dosen Sibuk", "Mengawas Ujian " + mkShort, "", "", null, bookedIntervals);
                }
            }
        }

        // 3. Check Student Conflict (BENTROK_MAHASISWA)
        Map<Integer, List<PesertaKelas>> pesertaByKelasId = new HashMap<>();
        Map<Integer, List<PesertaKelas>> pesertaByMatkulBukaId = new HashMap<>();
        for (PesertaKelas pk : ctx.pesertaMahasiswa()) {
            if (pk.getKelas() != null) {
                pesertaByKelasId.computeIfAbsent(pk.getKelas().getIdKelas(), k -> new ArrayList<>()).add(pk);
                if (pk.getKelas().getMataKuliahBuka() != null) {
                    pesertaByMatkulBukaId
                        .computeIfAbsent(pk.getKelas().getMataKuliahBuka().getIdMatkulBuka(), k -> new ArrayList<>())
                        .add(pk);
                }
            }
        }

        if ("REGULER".equals(mode)) {
            for (Kuliah kul : ctx.kuliahMahasiswaAllDays()) {
                if (kul.getHari() != null && kul.getHari() == hariKe
                    && !idKelas.equals(kul.getKelas().getIdKelas())
                    && isOverlapping(slotStart, slotEnd, kul.getJamMulai(), kul.getJamSelesai())) {
                    List<PesertaKelas> students = pesertaByKelasId.getOrDefault(kul.getKelas().getIdKelas(), Collections.emptyList());
                    if (!students.isEmpty()) {
                        String names = joinStudentFirstNames(students);
                        String mkShort = abbreviateCourseName(kul.getKelas().getMataKuliahBuka().getMatakuliah().getNama());
                        String agenda = "Kuliah " + mkShort;
                        return new RawCell(
                            "BENTROK_MAHASISWA",
                            "Mahasiswa Sibuk",
                            names + " (" + agenda + ")",
                            names,
                            agenda,
                            null,
                            bookedIntervals
                        );
                    }
                }
            }
        } else {
            for (Ujian u : ctx.ujianMahasiswaWeek()) {
                Integer mkbId = u.getMataKuliahBuka().getIdMatkulBuka();
                if (date.equals(u.getTanggal())
                    && mode.equalsIgnoreCase(u.getJenisUjian())
                    && (classMatkulBukaId == null || !classMatkulBukaId.equals(mkbId))
                    && isOverlapping(slotStart, slotEnd, u.getJamMulai(), u.getJamSelesai())) {
                    List<PesertaKelas> students = pesertaByMatkulBukaId.getOrDefault(mkbId, Collections.emptyList());
                    if (!students.isEmpty()) {
                        String names = joinStudentFirstNames(students);
                        String mkShort = abbreviateCourseName(u.getMataKuliahBuka().getMatakuliah().getNama());
                        String agenda = "Ujian " + u.getJenisUjian().toUpperCase() + " " + mkShort;
                        return new RawCell(
                            "BENTROK_MAHASISWA",
                            "Mahasiswa Sibuk",
                            names + " (" + agenda + ")",
                            names,
                            agenda,
                            null,
                            bookedIntervals
                        );
                    }
                }
            }
        }

        // Check if students have another presentation at the same time
        Map<Integer, List<AnggotaKelompok>> anggotaByKelompokAll = new HashMap<>();
        for (AnggotaKelompok ak : ctx.anggotaSemuaKelasMahasiswa()) {
            if (ak.getKelompok() != null) {
                anggotaByKelompokAll
                    .computeIfAbsent(ak.getKelompok().getIdKelompok(), k -> new ArrayList<>())
                    .add(ak);
            }
        }
        for (Presentasi p : ctx.presentasiWeekAll()) {
            if (p.getKelompok() == null || !date.equals(p.getTanggal())) {
                continue;
            }
            if (idKelompok != null && idKelompok.equals(p.getKelompok().getIdKelompok())) {
                continue;
            }
            List<AnggotaKelompok> affected = anggotaByKelompokAll.getOrDefault(
                p.getKelompok().getIdKelompok(), Collections.emptyList()
            );
            if (!affected.isEmpty() && isOverlapping(slotStart, slotEnd, p.getJamMulai(), p.getJamSelesai())) {
                Set<String> firstNames = new LinkedHashSet<>();
                for (AnggotaKelompok ak : affected) {
                    if (ak.getMahasiswa() != null && ak.getMahasiswa().getNama() != null) {
                        firstNames.add(extractFirstName(ak.getMahasiswa().getNama()));
                    }
                }
                String names = String.join(", ", firstNames);
                String mkShort = abbreviateCourseName(
                    p.getKelompok().getKelas().getMataKuliahBuka().getMatakuliah().getNama()
                );
                String agenda = "Presentasi " + mkShort;
                return new RawCell(
                    "BENTROK_MAHASISWA",
                    "Mahasiswa Sibuk",
                    names + " (" + agenda + ")",
                    names,
                    agenda,
                    null,
                    bookedIntervals
                );
            }
        }

        // 3. Check Class Priority Schedule (PRIORITAS_KELAS)
        if ("REGULER".equals(mode)) {
            for (Kuliah kul : ctx.kuliahKelasIni()) {
                if (kul.getHari() != null && kul.getHari() == hariKe
                    && isOverlapping(slotStart, slotEnd, kul.getJamMulai(), kul.getJamSelesai())) {
                    return new RawCell(
                        "PRIORITAS_KELAS",
                        "Jam Reguler " + namaSingkatMatkul,
                        "Aman, Semua Bisa Hadir",
                        "",
                        "",
                        null,
                        bookedIntervals
                    );
                }
            }
        } else if (classMatkulBukaId != null) {
            for (Ujian u : ctx.ujianMahasiswaWeek()) {
                if (date.equals(u.getTanggal())
                    && mode.equalsIgnoreCase(u.getJenisUjian())
                    && classMatkulBukaId.equals(u.getMataKuliahBuka().getIdMatkulBuka())
                    && isOverlapping(slotStart, slotEnd, u.getJamMulai(), u.getJamSelesai())) {
                    return new RawCell(
                        "PRIORITAS_KELAS",
                        "Jadwal Ujian " + namaSingkatMatkul,
                        "Aman, Semua Bisa Hadir",
                        "",
                        "",
                        null,
                        bookedIntervals
                    );
                }
            }
        }

        // 4. Default: KOSONG
        return new RawCell("KOSONG", "Kosong", "", "", "", null, bookedIntervals);
    }

    private List<BarisMatriksMingguanDTO> buildMergedRowsWithRowSpan(
        RawCell[][] grid,
        List<HariMingguanDTO> hariList
    ) {
        int numHours = 11;
        int numDays = 5;
        int[][] rowSpans = new int[numHours][numDays];
        boolean[][] skip = new boolean[numHours][numDays];
        LocalTime[][] blockEnd = new LocalTime[numHours][numDays];

        for (int d = 0; d < numDays; d++) {
            int h = 0;
            while (h < numHours) {
                RawCell current = grid[h][d];
                int span = 1;
                if (!"KOSONG".equals(current.status())) {
                    while (h + span < numHours && isSameMergeableBlock(current, grid[h + span][d])) {
                        skip[h + span][d] = true;
                        rowSpans[h + span][d] = 0;
                        span++;
                    }
                }
                rowSpans[h][d] = span;
                blockEnd[h][d] = LocalTime.of(7 + h + span, 0);
                h += span;
            }
        }

        List<BarisMatriksMingguanDTO> rows = new ArrayList<>(numHours);
        for (int h = 0; h < numHours; h++) {
            LocalTime rowStart = LocalTime.of(7 + h, 0);
            LocalTime rowEnd = LocalTime.of(8 + h, 0);
            String rowStartStr = rowStart.format(TIME_FMT);
            String rowEndStr = rowEnd.format(TIME_FMT);
            String labelWaktu = rowStartStr + " - " + rowEndStr;

            List<SelMatriksMingguanDTO> cells = new ArrayList<>(numDays);
            for (int d = 0; d < numDays; d++) {
                HariMingguanDTO hari = hariList.get(d);
                RawCell rc = grid[h][d];
                List<String> intervals = rc.intervalTerpakai() != null ? rc.intervalTerpakai() : Collections.emptyList();
                if (skip[h][d]) {
                    cells.add(new SelMatriksMingguanDTO(
                        hari.hariKe(),
                        hari.namaHari(),
                        hari.tanggal(),
                        hari.labelLengkap(),
                        rowStartStr,
                        rowEndStr,
                        labelWaktu,
                        rc.status(),
                        rc.badgeText(),
                        rc.deskripsi(),
                        rc.daftarMahasiswaBentrok(),
                        rc.namaAgendaBentrok(),
                        0,
                        true,
                        rc.idPresentasiTerjadwal(),
                        intervals
                    ));
                } else {
                    LocalTime effectiveEnd = blockEnd[h][d] != null ? blockEnd[h][d] : rowEnd;
                    String effectiveEndStr = effectiveEnd.format(TIME_FMT);
                    String blockRangeLabel = rowStartStr + " - " + effectiveEndStr;
                    cells.add(new SelMatriksMingguanDTO(
                        hari.hariKe(),
                        hari.namaHari(),
                        hari.tanggal(),
                        hari.labelLengkap(),
                        rowStartStr,
                        effectiveEndStr,
                        blockRangeLabel,
                        rc.status(),
                        rc.badgeText(),
                        rc.deskripsi(),
                        rc.daftarMahasiswaBentrok(),
                        rc.namaAgendaBentrok(),
                        rowSpans[h][d],
                        false,
                        rc.idPresentasiTerjadwal(),
                        intervals
                    ));
                }
            }

            rows.add(new BarisMatriksMingguanDTO(rowStartStr, rowEndStr, labelWaktu, cells));
        }
        return rows;
    }

    private boolean isSameMergeableBlock(RawCell a, RawCell b) {
        if (a == null || b == null) return false;
        return Objects.equals(a.status(), b.status())
            && Objects.equals(a.badgeText(), b.badgeText())
            && Objects.equals(a.deskripsi(), b.deskripsi());
    }

    private String joinStudentFirstNames(List<PesertaKelas> pesertaList) {
        Set<String> names = new LinkedHashSet<>();
        for (PesertaKelas pk : pesertaList) {
            if (pk.getMahasiswa() != null && pk.getMahasiswa().getNama() != null) {
                names.add(extractFirstName(pk.getMahasiswa().getNama()));
            }
        }
        return String.join(", ", names);
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Mahasiswa";
        String trimmed = fullName.trim();
        int spaceIdx = trimmed.indexOf(' ');
        return spaceIdx > 0 ? trimmed.substring(0, spaceIdx) : trimmed;
    }

    private String resolveNamaMatkul(Presentasi p, Kelas fallbackKelas) {
        Kelas k = (p != null && p.getKelompok() != null && p.getKelompok().getKelas() != null)
            ? p.getKelompok().getKelas()
            : fallbackKelas;
        if (k != null && k.getMataKuliahBuka() != null && k.getMataKuliahBuka().getMatakuliah() != null) {
            String nama = k.getMataKuliahBuka().getMatakuliah().getNama();
            if (nama != null && !nama.isBlank()) {
                return nama;
            }
        }
        return "Mata Kuliah";
    }

    private String abbreviateCourseName(String namaMatkul) {
        if (namaMatkul == null || namaMatkul.isBlank()) return "MK";
        String lower = namaMatkul.toLowerCase();
        if (lower.contains("jaringan komputer")) return "Jarkom";
        if (lower.contains("algoritma dan struktur data") || lower.contains("struktur data")) return "ASD";
        if (lower.contains("rekayasa perangkat lunak") || lower.contains("pembangunan perangkat lunak")) return "RPL";
        if (lower.contains("sistem operasi")) return "Sisop";
        if (lower.contains("manajemen proyek")) return "Mpro";
        if (lower.contains("pemrograman berorientasi objek")) return "PBO";
        if (lower.contains("back end")) return "Back End";
        if (lower.contains("basis data")) return "Basdat";
        String[] words = namaMatkul.trim().split("\\s+");
        if (words.length == 1) return words[0];
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty() && Character.isUpperCase(w.charAt(0)) && !"dan".equalsIgnoreCase(w)) {
                sb.append(w.charAt(0));
            }
        }
        return sb.length() >= 2 ? sb.toString() : words[0];
    }

    private String resolveDefaultRuanganKelas(List<Kuliah> kuliahList) {
        if (kuliahList != null) {
            for (Kuliah k : kuliahList) {
                if (k.getRuangan() != null && !k.getRuangan().isBlank() && !"-".equals(k.getRuangan())) {
                    return k.getRuangan().trim();
                }
            }
        }
        return DEFAULT_RUANGAN;
    }

    @Transactional(readOnly = true)
    public MatriksPenjadwalanResponseDTO getMatriksHarian(
        Integer idKelas,
        String ruangan,
        LocalDate tanggal,
        Integer idKelompokFilter,
        String dosenId
    ) {
        Integer targetKelas = idKelas != null ? idKelas : 102;
        String targetRuangan = (ruangan != null && !ruangan.isBlank()) ? ruangan.trim() : DEFAULT_RUANGAN;
        LocalDate targetTanggal = tanggal != null ? tanggal : LocalDate.now();

        List<Kelompok> dbKelompok = kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(targetKelas);
        Optional<Kelas> dbKelasOpt = kelasRepository.findByIdWithDetails(targetKelas);

        if (dbKelasOpt.isEmpty() && dbKelompok.isEmpty()) {
            return buildFallbackMatriks(targetKelas, targetRuangan, targetTanggal, idKelompokFilter);
        }

        BatchConflictContext context = loadBatchConflictContext(
            targetKelas,
            dbKelasOpt.orElse(null),
            dbKelompok,
            idKelompokFilter,
            targetRuangan,
            targetTanggal,
            dosenId
        );

        String namaKelas = dbKelasOpt
            .map(k -> k.getMataKuliahBuka().getMatakuliah().getIdMatkul() + " - "
                + k.getMataKuliahBuka().getMatakuliah().getNama() + " (Kelas " + k.getKodeKelas() + ")")
            .orElse("IF3130 - Jaringan Komputer (Kelas B)");

        List<SlotMatriksDTO> slots = new ArrayList<>();
        for (int hour = 7; hour < 17; hour++) {
            LocalTime slotStart = LocalTime.of(hour, 0);
            LocalTime slotEnd = LocalTime.of(hour + 1, 0);
            String startStr = slotStart.format(TIME_FMT);
            String endStr = slotEnd.format(TIME_FMT);
            String label = startStr + " - " + endStr;

            Presentasi matchingPresentasi = findClassPresentasiInSlot(context, targetKelas, targetRuangan, slotStart, slotEnd);
            if (matchingPresentasi != null) {
                PresentasiTerjadwalDTO presDto = toPresentasiTerjadwalDTO(matchingPresentasi, context);
                slots.add(new SlotMatriksDTO(
                    startStr,
                    endStr,
                    label,
                    "TERISI",
                    null,
                    Collections.emptyList(),
                    presDto
                ));
                continue;
            }

            List<KonflikDetailDTO> conflicts = evaluateSlotConflicts(
                context,
                idKelompokFilter,
                targetRuangan,
                slotStart,
                slotEnd,
                false
            );

            if (!conflicts.isEmpty()) {
                slots.add(new SlotMatriksDTO(
                    startStr,
                    endStr,
                    label,
                    "BENTROK",
                    conflicts.get(0).deskripsi(),
                    conflicts,
                    null
                ));
            } else {
                slots.add(new SlotMatriksDTO(
                    startStr,
                    endStr,
                    label,
                    "TERSEDIA",
                    null,
                    Collections.emptyList(),
                    null
                ));
            }
        }

        return new MatriksPenjadwalanResponseDTO(
            targetKelas,
            namaKelas,
            targetRuangan,
            targetTanggal.toString(),
            resolveHariIndonesia(targetTanggal.getDayOfWeek().getValue()),
            slots
        );
    }

    @Transactional(readOnly = true)
    public CekBentrokResponseDTO cekBentrok(CekBentrokRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Permintaan pengecekan bentrok tidak boleh kosong.");
        }
        Integer idKelas = request.idKelas() != null ? request.idKelas() : 102;
        Integer idKelompok = request.idKelompok();
        String ruangan = (request.ruangan() != null && !request.ruangan().isBlank()) ? request.ruangan().trim() : DEFAULT_RUANGAN;
        LocalDate tanggal = parseDateOrDefault(request.tanggal(), LocalDate.now());
        LocalTime jamMulai = parseTimeOrDefault(request.jamMulai(), LocalTime.of(8, 0));
        int durasi = (request.durasiMenit() != null && request.durasiMenit() > 0) ? request.durasiMenit() : 30;
        LocalTime jamSelesai = jamMulai.plusMinutes(durasi);

        List<Kelompok> dbKelompok = kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(idKelas);
        Optional<Kelas> dbKelasOpt = kelasRepository.findByIdWithDetails(idKelas);

        if (dbKelasOpt.isEmpty() && dbKelompok.isEmpty()) {
            return cekBentrokFallback(idKelas, idKelompok, ruangan, tanggal, jamMulai, jamSelesai, durasi);
        }

        BatchConflictContext context = loadBatchConflictContext(
            idKelas,
            dbKelasOpt.orElse(null),
            dbKelompok,
            idKelompok,
            ruangan,
            tanggal,
            request.dosenId()
        );

        List<KonflikDetailDTO> conflicts = evaluateSlotConflicts(
            context,
            idKelompok,
            ruangan,
            jamMulai,
            jamSelesai,
            true
        );

        return new CekBentrokResponseDTO(
            !conflicts.isEmpty(),
            jamMulai.format(TIME_FMT),
            jamSelesai.format(TIME_FMT),
            durasi,
            conflicts
        );
    }

    public PresentasiTerjadwalDTO simpanPresentasi(SimpanPresentasiRequestDTO request) {
        if (request == null || request.idKelompok() == null) {
            throw new IllegalArgumentException("Kelompok wajib dipilih sebelum menyimpan jadwal presentasi.");
        }

        Optional<Kelompok> kelompokOpt = kelompokRepository.findById(request.idKelompok());
        Integer idKelas = request.idKelas();
        if (idKelas == null && kelompokOpt.isPresent() && kelompokOpt.get().getKelas() != null) {
            idKelas = kelompokOpt.get().getKelas().getIdKelas();
        }
        if (idKelas == null) {
            idKelas = (request.idKelompok() >= 10) ? 101 : 102;
        }

        String rawRoom = (request.ruangan() != null && !request.ruangan().isBlank())
            ? request.ruangan()
            : request.idRuangan();
        String ruangan = (rawRoom != null && !rawRoom.isBlank()) ? rawRoom.trim() : DEFAULT_RUANGAN;

        LocalDate tanggal = parseDateOrDefault(request.tanggal(), DEFAULT_REGULER_MONDAY);
        LocalTime jamMulai = parseTimeOrDefault(request.jamMulai(), LocalTime.of(8, 0));

        int durasi = (request.durasiMenit() != null && request.durasiMenit() > 0) ? request.durasiMenit() : 30;
        LocalTime jamSelesai = (request.jamSelesai() != null && !request.jamSelesai().isBlank())
            ? parseTimeOrDefault(request.jamSelesai(), jamMulai.plusMinutes(durasi))
            : jamMulai.plusMinutes(durasi);

        if (jamSelesai.isAfter(jamMulai)) {
            durasi = computeDurationMinutes(jamMulai, jamSelesai, durasi);
        } else {
            jamSelesai = jamMulai.plusMinutes(durasi);
        }

        boolean forceOverride = Boolean.TRUE.equals(request.forceOverride());

        CekBentrokResponseDTO hasilCek = cekBentrok(new CekBentrokRequestDTO(
            idKelas,
            request.idKelompok(),
            ruangan,
            tanggal.toString(),
            jamMulai.format(TIME_FMT),
            durasi,
            request.dosenId()
        ));

        if (hasilCek.adaBentrok()) {
            // Even when forceOverride == true, never allow double-booking the exact same group (KELOMPOK)
            // or overriding a lecturer conflict (DOSEN). Partial student conflicts (MAHASISWA) can be overridden.
            List<KonflikDetailDTO> blockingConflicts = new ArrayList<>();
            for (KonflikDetailDTO k : hasilCek.konflikList()) {
                if (!forceOverride) {
                    blockingConflicts.add(k);
                } else if ("KELOMPOK".equals(k.dimensi()) || "DOSEN".equals(k.dimensi())) {
                    blockingConflicts.add(k);
                }
            }

            if (!blockingConflicts.isEmpty()) {
                String primaryReason = blockingConflicts.get(0).deskripsi();
                throw new ConflictScheduleException(
                    primaryReason,
                    new CekBentrokResponseDTO(
                        true,
                        jamMulai.format(TIME_FMT),
                        jamSelesai.format(TIME_FMT),
                        durasi,
                        blockingConflicts
                    )
                );
            }
        }

        if (kelompokOpt.isEmpty()) {
            return simpanFallbackPresentasi(
                idKelas, request.idKelompok(), ruangan, tanggal, jamMulai, jamSelesai, durasi, request.catatan()
            );
        }

        Kelompok kelompok = kelompokOpt.get();
        Presentasi presentasi = new Presentasi();
        presentasi.setKelompok(kelompok);
        presentasi.setTanggal(tanggal);
        presentasi.setJamMulai(jamMulai);
        presentasi.setJamSelesai(jamSelesai);
        presentasi.setRuangan(ruangan);
        presentasi.setAdaBentrok(hasilCek.adaBentrok());
        presentasi.setCatatan(request.catatan() != null && !request.catatan().isBlank() ? request.catatan().trim() : null);

        Presentasi saved = presentasiRepository.save(presentasi);

        List<AnggotaKelompok> anggotaEntities = anggotaKelompokRepository.findByKelompok_IdKelompok(kelompok.getIdKelompok());
        List<AnggotaItemDTO> anggotaDtos = new ArrayList<>();
        for (AnggotaKelompok ak : anggotaEntities) {
            if (ak.getMahasiswa() != null) {
                anggotaDtos.add(new AnggotaItemDTO(ak.getMahasiswa().getNpm(), ak.getMahasiswa().getNama()));
            }
        }

        Kelas kls = kelompok.getKelas();
        String kodeKelas = kls != null ? kls.getKodeKelas() : "B";
        String namaMatkul = (kls != null && kls.getMataKuliahBuka() != null && kls.getMataKuliahBuka().getMatakuliah() != null)
            ? kls.getMataKuliahBuka().getMatakuliah().getNama()
            : "Mata Kuliah";

        return new PresentasiTerjadwalDTO(
            saved.getIdPresentasi(),
            kelompok.getIdKelompok(),
            kelompok.getNoKelompok() != null ? kelompok.getNoKelompok().intValue() : 1,
            kelompok.getTopik() != null ? kelompok.getTopik() : ("Kelompok " + kelompok.getNoKelompok()),
            saved.getTanggal().toString(),
            saved.getJamMulai().format(TIME_FMT),
            saved.getJamSelesai().format(TIME_FMT),
            saved.getRuangan(),
            durasi,
            saved.getCatatan(),
            kodeKelas,
            namaMatkul,
            anggotaDtos
        );
    }

    public void hapusPresentasi(Integer idPresentasi) {
        if (idPresentasi == null) {
            throw new IllegalArgumentException("ID jadwal presentasi tidak valid.");
        }
        if (fallbackPresentasiById.containsKey(idPresentasi)) {
            fallbackPresentasiById.remove(idPresentasi);
            return;
        }
        if (presentasiRepository.existsById(idPresentasi)) {
            presentasiRepository.deleteById(idPresentasi);
        }
    }

    /**
     * Batch-loads all 4 dimensions of schedule data in fixed queries without N+1 loops.
     */
    private BatchConflictContext loadBatchConflictContext(
        Integer idKelas,
        Kelas kelas,
        List<Kelompok> kelompokList,
        Integer idKelompokFilter,
        String ruangan,
        LocalDate tanggal,
        String requestedDosenId
    ) {
        short hari = (short) tanggal.getDayOfWeek().getValue();

        Set<String> dosenIds = new LinkedHashSet<>();
        if (requestedDosenId != null && !requestedDosenId.isBlank()) {
            dosenIds.add(requestedDosenId.trim());
        }
        if (kelas != null) {
            if (kelas.getPenanggungJawab() != null && kelas.getPenanggungJawab().getNomorIndukDosen() != null) {
                dosenIds.add(kelas.getPenanggungJawab().getNomorIndukDosen());
            }
            if (kelas.getMataKuliahBuka() != null
                && kelas.getMataKuliahBuka().getKoordinator() != null
                && kelas.getMataKuliahBuka().getKoordinator().getNomorIndukDosen() != null) {
                dosenIds.add(kelas.getMataKuliahBuka().getKoordinator().getNomorIndukDosen());
            }
        }
        List<Kuliah> kuliahKelasIni = kuliahRepository.findByKelas_IdKelas(idKelas);
        for (Kuliah kul : kuliahKelasIni) {
            if (kul.getDosenPengajar() != null && kul.getDosenPengajar().getNomorIndukDosen() != null) {
                dosenIds.add(kul.getDosenPengajar().getNomorIndukDosen());
            }
        }
        if (dosenIds.isEmpty()) {
            dosenIds.add(DEFAULT_NIDN);
        }

        List<Kuliah> kuliahDosen = kuliahRepository.findByHariAndDosenIdsWithDetails(hari, dosenIds);
        List<PengawasUjian> pengawasDosen = pengawasUjianRepository.findByTanggalAndDosenIdsWithDetails(tanggal, dosenIds);
        List<Presentasi> presentasiTanggalAll = presentasiRepository.findByTanggalWithDetails(tanggal);

        List<AnggotaKelompok> anggotaKelas = anggotaKelompokRepository.findByKelasIdWithMahasiswaAndKelompok(idKelas);
        Map<Integer, List<AnggotaItemDTO>> anggotaByKelompokId = new HashMap<>();
        Set<String> targetNpms = new LinkedHashSet<>();
        for (AnggotaKelompok ak : anggotaKelas) {
            if (ak.getKelompok() == null || ak.getMahasiswa() == null) {
                continue;
            }
            Integer kelId = ak.getKelompok().getIdKelompok();
            anggotaByKelompokId
                .computeIfAbsent(kelId, k -> new ArrayList<>())
                .add(new AnggotaItemDTO(ak.getMahasiswa().getNpm(), ak.getMahasiswa().getNama()));

            if (idKelompokFilter == null || idKelompokFilter.equals(kelId)) {
                targetNpms.add(ak.getMahasiswa().getNpm());
            }
        }

        List<PesertaKelas> pesertaMahasiswa = targetNpms.isEmpty()
            ? Collections.emptyList()
            : pesertaKelasRepository.findByMahasiswaNpmInWithDetails(targetNpms);

        Set<Integer> studentKelasIds = new LinkedHashSet<>();
        Set<Integer> studentMatkulBukaIds = new LinkedHashSet<>();
        for (PesertaKelas pk : pesertaMahasiswa) {
            if (pk.getKelas() != null) {
                studentKelasIds.add(pk.getKelas().getIdKelas());
                if (pk.getKelas().getMataKuliahBuka() != null) {
                    studentMatkulBukaIds.add(pk.getKelas().getMataKuliahBuka().getIdMatkulBuka());
                }
            }
        }

        List<Kuliah> kuliahMahasiswa = studentKelasIds.isEmpty()
            ? Collections.emptyList()
            : kuliahRepository.findByHariAndKelasIdsWithDetails(hari, studentKelasIds);

        List<Ujian> ujianMahasiswa = studentMatkulBukaIds.isEmpty()
            ? Collections.emptyList()
            : ujianRepository.findByTanggalAndMatkulBukaIds(tanggal, studentMatkulBukaIds);

        List<AnggotaKelompok> anggotaSemuaKelasMahasiswa = targetNpms.isEmpty()
            ? Collections.emptyList()
            : anggotaKelompokRepository.findByMahasiswaNpmInWithDetails(targetNpms);

        Set<Integer> studentKelompokIds = new LinkedHashSet<>();
        for (AnggotaKelompok ak : anggotaSemuaKelasMahasiswa) {
            if (ak.getKelompok() != null) {
                studentKelompokIds.add(ak.getKelompok().getIdKelompok());
            }
        }

        List<Presentasi> presentasiMahasiswa = studentKelompokIds.isEmpty()
            ? Collections.emptyList()
            : presentasiRepository.findByTanggalAndKelompokIdsWithDetails(tanggal, studentKelompokIds);

        List<Kuliah> kuliahRuangan = kuliahRepository.findByHariAndRuanganWithDetails(hari, ruangan);
        List<Ujian> ujianRuangan = ujianRepository.findByTanggalAndRuanganWithDetails(tanggal, ruangan);
        List<Presentasi> presentasiRuangan = presentasiRepository.findByTanggalAndRuanganWithDetails(tanggal, ruangan);

        List<Presentasi> presentasiKelasIni = presentasiRepository.findByKelasIdWithDetails(idKelas);
        Map<Integer, Presentasi> presentasiByKelompokId = new HashMap<>();
        for (Presentasi p : presentasiKelasIni) {
            if (p.getKelompok() != null) {
                presentasiByKelompokId.put(p.getKelompok().getIdKelompok(), p);
            }
        }

        Map<Integer, Kelompok> kelompokById = new HashMap<>();
        for (Kelompok k : kelompokList) {
            kelompokById.put(k.getIdKelompok(), k);
        }

        return new BatchConflictContext(
            idKelas,
            dosenIds,
            kelompokById,
            anggotaByKelompokId,
            kuliahDosen,
            pengawasDosen,
            presentasiTanggalAll,
            pesertaMahasiswa,
            kuliahMahasiswa,
            ujianMahasiswa,
            anggotaSemuaKelasMahasiswa,
            presentasiMahasiswa,
            kuliahRuangan,
            ujianRuangan,
            presentasiRuangan,
            presentasiByKelompokId
        );
    }

    private Presentasi findClassPresentasiInSlot(
        BatchConflictContext context,
        Integer idKelas,
        String ruangan,
        LocalTime slotStart,
        LocalTime slotEnd
    ) {
        for (Presentasi p : context.presentasiRuangan()) {
            if (p.getKelompok() != null
                && p.getKelompok().getKelas() != null
                && idKelas.equals(p.getKelompok().getKelas().getIdKelas())
                && p.getRuangan() != null
                && p.getRuangan().equalsIgnoreCase(ruangan)
                && isOverlapping(slotStart, slotEnd, p.getJamMulai(), p.getJamSelesai())) {
                return p;
            }
        }
        return null;
    }

    private List<KonflikDetailDTO> evaluateSlotConflicts(
        BatchConflictContext context,
        Integer idKelompok,
        String ruangan,
        LocalTime jamMulai,
        LocalTime jamSelesai,
        boolean checkKelompokAlreadyScheduled
    ) {
        List<KonflikDetailDTO> conflicts = new ArrayList<>();
        Set<String> dedupKeys = new LinkedHashSet<>();

        // DIMENSI 4: Kelompok
        if (checkKelompokAlreadyScheduled && idKelompok != null) {
            Presentasi existing = context.presentasiByKelompokId().get(idKelompok);
            if (existing != null) {
                Kelompok kel = context.kelompokById().get(idKelompok);
                int noKel = (kel != null && kel.getNoKelompok() != null) ? kel.getNoKelompok().intValue() : 1;
                String desc = String.format(
                    "Bentrok Kelompok: Kelompok %d sudah memiliki jadwal presentasi pada %s (%s - %s di %s)",
                    noKel,
                    existing.getTanggal(),
                    existing.getJamMulai().format(TIME_FMT),
                    existing.getJamSelesai().format(TIME_FMT),
                    existing.getRuangan()
                );
                addConflict(conflicts, dedupKeys, "KELOMPOK", "Bentrok Kelompok", desc);
            }
        }

        // DIMENSI 3: Ruangan (exclude the current class's own regular lecture slot so Priority Slot is allowed!)
        for (Kuliah kul : context.kuliahRuangan()) {
            if (context.idKelas().equals(kul.getKelas().getIdKelas())) {
                continue;
            }
            if (isOverlapping(jamMulai, jamSelesai, kul.getJamMulai(), kul.getJamSelesai())) {
                String kodeMk = kul.getKelas().getMataKuliahBuka().getMatakuliah().getIdMatkul();
                String namaMk = kul.getKelas().getMataKuliahBuka().getMatakuliah().getNama();
                String desc = String.format(
                    "Bentrok Ruangan: %s terpakai kuliah reguler %s %s (%s - %s)",
                    ruangan,
                    kodeMk,
                    namaMk,
                    kul.getJamMulai().format(TIME_FMT),
                    kul.getJamSelesai().format(TIME_FMT)
                );
                addConflict(conflicts, dedupKeys, "RUANGAN", "Bentrok Ruangan", desc);
            }
        }

        for (Ujian u : context.ujianRuangan()) {
            if (isOverlapping(jamMulai, jamSelesai, u.getJamMulai(), u.getJamSelesai())) {
                String kodeMk = u.getMataKuliahBuka().getMatakuliah().getIdMatkul();
                String namaMk = u.getMataKuliahBuka().getMatakuliah().getNama();
                String desc = String.format(
                    "Bentrok Ruangan: %s terpakai %s %s %s (%s - %s)",
                    ruangan,
                    u.getJenisUjian(),
                    kodeMk,
                    namaMk,
                    u.getJamMulai().format(TIME_FMT),
                    u.getJamSelesai().format(TIME_FMT)
                );
                addConflict(conflicts, dedupKeys, "RUANGAN", "Bentrok Ruangan", desc);
            }
        }

        for (Presentasi p : context.presentasiRuangan()) {
            if (idKelompok != null && p.getKelompok() != null && idKelompok.equals(p.getKelompok().getIdKelompok())) {
                continue;
            }
            if (isOverlapping(jamMulai, jamSelesai, p.getJamMulai(), p.getJamSelesai())) {
                String kodeMk = p.getKelompok().getKelas().getMataKuliahBuka().getMatakuliah().getIdMatkul();
                int noKel = p.getKelompok().getNoKelompok() != null ? p.getKelompok().getNoKelompok().intValue() : 1;
                String desc = String.format(
                    "Bentrok Ruangan: %s telah digunakan presentasi Kelompok %d %s (%s - %s)",
                    ruangan,
                    noKel,
                    kodeMk,
                    p.getJamMulai().format(TIME_FMT),
                    p.getJamSelesai().format(TIME_FMT)
                );
                addConflict(conflicts, dedupKeys, "RUANGAN", "Bentrok Ruangan", desc);
            }
        }

        // DIMENSI 1: Dosen Pengampu Kelas (exclude the current class's own regular lecture slot)
        for (Kuliah kul : context.kuliahDosen()) {
            if (context.idKelas().equals(kul.getKelas().getIdKelas())) {
                continue;
            }
            if (isOverlapping(jamMulai, jamSelesai, kul.getJamMulai(), kul.getJamSelesai())) {
                String namaDosen = kul.getDosenPengajar() != null ? kul.getDosenPengajar().getNama() : "Dosen Pengampu";
                String kodeMk = kul.getKelas().getMataKuliahBuka().getMatakuliah().getIdMatkul();
                String namaMk = kul.getKelas().getMataKuliahBuka().getMatakuliah().getNama();
                String desc = String.format(
                    "Bentrok Dosen: %s memiliki jadwal mengajar %s %s (%s - %s)",
                    namaDosen,
                    kodeMk,
                    namaMk,
                    kul.getJamMulai().format(TIME_FMT),
                    kul.getJamSelesai().format(TIME_FMT)
                );
                addConflict(conflicts, dedupKeys, "DOSEN", "Bentrok Dosen", desc);
            }
        }

        for (PengawasUjian pu : context.pengawasDosen()) {
            Ujian u = pu.getUjian();
            if (u != null && isOverlapping(jamMulai, jamSelesai, u.getJamMulai(), u.getJamSelesai())) {
                String namaDosen = pu.getDosen() != null ? pu.getDosen().getNama() : "Dosen Pengampu";
                String kodeMk = u.getMataKuliahBuka().getMatakuliah().getIdMatkul();
                String desc = String.format(
                    "Bentrok Dosen: %s bertugas mengawas ujian %s %s (%s - %s)",
                    namaDosen,
                    u.getJenisUjian(),
                    kodeMk,
                    u.getJamMulai().format(TIME_FMT),
                    u.getJamSelesai().format(TIME_FMT)
                );
                addConflict(conflicts, dedupKeys, "DOSEN", "Bentrok Dosen", desc);
            }
        }

        for (Presentasi p : context.presentasiTanggalAll()) {
            if (p.getKelompok() == null || p.getKelompok().getKelas() == null) {
                continue;
            }
            if (idKelompok != null && idKelompok.equals(p.getKelompok().getIdKelompok())) {
                continue;
            }
            Kelas pKelas = p.getKelompok().getKelas();
            boolean taughtBySameLecturer = false;
            if (pKelas.getPenanggungJawab() != null
                && context.dosenIds().contains(pKelas.getPenanggungJawab().getNomorIndukDosen())) {
                taughtBySameLecturer = true;
            } else if (pKelas.getMataKuliahBuka() != null
                && pKelas.getMataKuliahBuka().getKoordinator() != null
                && context.dosenIds().contains(pKelas.getMataKuliahBuka().getKoordinator().getNomorIndukDosen())) {
                taughtBySameLecturer = true;
            }

            if (taughtBySameLecturer && isOverlapping(jamMulai, jamSelesai, p.getJamMulai(), p.getJamSelesai())) {
                String kodeMk = pKelas.getMataKuliahBuka().getMatakuliah().getIdMatkul();
                int noKel = p.getKelompok().getNoKelompok() != null ? p.getKelompok().getNoKelompok().intValue() : 1;
                String desc = String.format(
                    "Bentrok Dosen: Menguji presentasi Kelompok %d pada kelas %s (%s - %s)",
                    noKel,
                    kodeMk,
                    p.getJamMulai().format(TIME_FMT),
                    p.getJamSelesai().format(TIME_FMT)
                );
                addConflict(conflicts, dedupKeys, "DOSEN", "Bentrok Dosen", desc);
            }
        }

        // DIMENSI 2: Mahasiswa Anggota Kelompok (exclude the current class's own regular lecture slot)
        Map<Integer, List<PesertaKelas>> pesertaByKelasId = new HashMap<>();
        Map<Integer, List<PesertaKelas>> pesertaByMatkulBukaId = new HashMap<>();
        for (PesertaKelas pk : context.pesertaMahasiswa()) {
            if (pk.getKelas() != null) {
                pesertaByKelasId.computeIfAbsent(pk.getKelas().getIdKelas(), k -> new ArrayList<>()).add(pk);
                if (pk.getKelas().getMataKuliahBuka() != null) {
                    pesertaByMatkulBukaId
                        .computeIfAbsent(pk.getKelas().getMataKuliahBuka().getIdMatkulBuka(), k -> new ArrayList<>())
                        .add(pk);
                }
            }
        }

        for (Kuliah kul : context.kuliahMahasiswa()) {
            if (context.idKelas().equals(kul.getKelas().getIdKelas())) {
                continue;
            }
            if (isOverlapping(jamMulai, jamSelesai, kul.getJamMulai(), kul.getJamSelesai())) {
                List<PesertaKelas> affectedStudents = pesertaByKelasId.getOrDefault(kul.getKelas().getIdKelas(), Collections.emptyList());
                String kodeMk = kul.getKelas().getMataKuliahBuka().getMatakuliah().getIdMatkul();
                for (PesertaKelas pk : affectedStudents) {
                    String namaMhs = pk.getMahasiswa() != null ? pk.getMahasiswa().getNama() : "Mahasiswa";
                    String desc = String.format(
                        "Bentrok Mahasiswa: %s memiliki jadwal kuliah %s (%s - %s)",
                        namaMhs,
                        kodeMk,
                        kul.getJamMulai().format(TIME_FMT),
                        kul.getJamSelesai().format(TIME_FMT)
                    );
                    addConflict(conflicts, dedupKeys, "MAHASISWA", "Bentrok Mahasiswa", desc);
                }
            }
        }

        for (Ujian u : context.ujianMahasiswa()) {
            if (isOverlapping(jamMulai, jamSelesai, u.getJamMulai(), u.getJamSelesai())) {
                Integer mkbId = u.getMataKuliahBuka().getIdMatkulBuka();
                List<PesertaKelas> affectedStudents = pesertaByMatkulBukaId.getOrDefault(mkbId, Collections.emptyList());
                String kodeMk = u.getMataKuliahBuka().getMatakuliah().getIdMatkul();
                for (PesertaKelas pk : affectedStudents) {
                    String namaMhs = pk.getMahasiswa() != null ? pk.getMahasiswa().getNama() : "Mahasiswa";
                    String desc = String.format(
                        "Bentrok Mahasiswa: %s memiliki jadwal %s %s (%s - %s)",
                        namaMhs,
                        u.getJenisUjian(),
                        kodeMk,
                        u.getJamMulai().format(TIME_FMT),
                        u.getJamSelesai().format(TIME_FMT)
                    );
                    addConflict(conflicts, dedupKeys, "MAHASISWA", "Bentrok Mahasiswa", desc);
                }
            }
        }

        Map<Integer, List<AnggotaKelompok>> anggotaByKelompokAll = new HashMap<>();
        for (AnggotaKelompok ak : context.anggotaSemuaKelasMahasiswa()) {
            if (ak.getKelompok() != null) {
                anggotaByKelompokAll
                    .computeIfAbsent(ak.getKelompok().getIdKelompok(), k -> new ArrayList<>())
                    .add(ak);
            }
        }

        for (Presentasi p : context.presentasiMahasiswa()) {
            if (p.getKelompok() == null) {
                continue;
            }
            if (idKelompok != null && idKelompok.equals(p.getKelompok().getIdKelompok())) {
                continue;
            }
            if (isOverlapping(jamMulai, jamSelesai, p.getJamMulai(), p.getJamSelesai())) {
                String kodeMk = p.getKelompok().getKelas().getMataKuliahBuka().getMatakuliah().getIdMatkul();
                List<AnggotaKelompok> affected = anggotaByKelompokAll.getOrDefault(p.getKelompok().getIdKelompok(), Collections.emptyList());
                for (AnggotaKelompok ak : affected) {
                    String namaMhs = ak.getMahasiswa() != null ? ak.getMahasiswa().getNama() : "Mahasiswa";
                    String desc = String.format(
                        "Bentrok Mahasiswa: %s memiliki jadwal presentasi pada mata kuliah %s (%s - %s)",
                        namaMhs,
                        kodeMk,
                        p.getJamMulai().format(TIME_FMT),
                        p.getJamSelesai().format(TIME_FMT)
                    );
                    addConflict(conflicts, dedupKeys, "MAHASISWA", "Bentrok Mahasiswa", desc);
                }
            }
        }

        return conflicts;
    }

    private void addConflict(
        List<KonflikDetailDTO> list,
        Set<String> seenKeys,
        String dimensi,
        String ringkasan,
        String deskripsi
    ) {
        String key = dimensi + "::" + deskripsi;
        if (seenKeys.add(key)) {
            list.add(new KonflikDetailDTO(dimensi, ringkasan, deskripsi));
        }
    }

    private PresentasiTerjadwalDTO toPresentasiTerjadwalDTO(Presentasi p, BatchConflictContext context) {
        Kelompok kel = p.getKelompok();
        Integer idKel = kel.getIdKelompok();
        int nomor = kel.getNoKelompok() != null ? kel.getNoKelompok().intValue() : 1;
        String topik = (kel.getTopik() != null && !kel.getTopik().isBlank())
            ? kel.getTopik().trim()
            : ("Kelompok " + nomor);
        int durasi = computeDurationMinutes(p.getJamMulai(), p.getJamSelesai(), 30);
        String kodeKelas = kel.getKelas() != null ? kel.getKelas().getKodeKelas() : "B";
        String namaMatkul = (kel.getKelas() != null
            && kel.getKelas().getMataKuliahBuka() != null
            && kel.getKelas().getMataKuliahBuka().getMatakuliah() != null)
            ? kel.getKelas().getMataKuliahBuka().getMatakuliah().getNama()
            : "Mata Kuliah";

        List<AnggotaItemDTO> anggota = context.anggotaByKelompokId().getOrDefault(idKel, Collections.emptyList());

        return new PresentasiTerjadwalDTO(
            p.getIdPresentasi(),
            idKel,
            nomor,
            topik,
            p.getTanggal().toString(),
            p.getJamMulai().format(TIME_FMT),
            p.getJamSelesai().format(TIME_FMT),
            p.getRuangan(),
            durasi,
            p.getCatatan(),
            kodeKelas,
            namaMatkul,
            anggota
        );
    }

    private int computeDurationMinutes(LocalTime start, LocalTime end, int defaultMinutes) {
        if (start == null || end == null || !end.isAfter(start)) {
            return defaultMinutes;
        }
        return (int) Duration.between(start, end).toMinutes();
    }

    private LocalDate parseDateOrDefault(String rawDate, LocalDate fallback) {
        if (rawDate == null || rawDate.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(rawDate.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private LocalTime parseTimeOrDefault(String rawTime, LocalTime fallback) {
        if (rawTime == null || rawTime.isBlank()) {
            return fallback;
        }
        try {
            String trimmed = rawTime.trim();
            if (trimmed.length() == 5) {
                return LocalTime.parse(trimmed, TIME_FMT);
            }
            return LocalTime.parse(trimmed);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String normalizeMode(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return "REGULER";
        }
        String upper = rawMode.trim().toUpperCase();
        if ("UTS".equals(upper) || "UAS".equals(upper)) {
            return upper;
        }
        return "REGULER";
    }

    private LocalDate resolveWeekMonday(String mode, LocalDate requestedDate) {
        if (requestedDate != null) {
            return requestedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        return switch (mode) {
            case "UTS" -> DEFAULT_UTS_MONDAY;
            case "UAS" -> DEFAULT_UAS_MONDAY;
            default -> DEFAULT_REGULER_MONDAY;
        };
    }

    private String resolveHariIndonesia(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Senin";
            case 2 -> "Selasa";
            case 3 -> "Rabu";
            case 4 -> "Kamis";
            case 5 -> "Jumat";
            case 6 -> "Sabtu";
            case 7 -> "Minggu";
            default -> "Senin";
        };
    }

    private String formatDayMonthShortIndo(LocalDate date) {
        String[] shortMonths = {
            "", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
        };
        return String.format("%02d %s", date.getDayOfMonth(), shortMonths[date.getMonthValue()]);
    }

    private String formatDateFullIndo(LocalDate date) {
        String[] fullMonths = {
            "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        };
        return String.format("%02d %s %d", date.getDayOfMonth(), fullMonths[date.getMonthValue()], date.getYear());
    }

    // =========================================================================
    // Fallback Support when PostgreSQL tables are empty/unseeded
    // =========================================================================

    private List<KelompokPenjadwalanDTO> getFallbackKelompokByKelas(Integer idKelas) {
        Map<Integer, PresentasiTerjadwalDTO> scheduledByGroup = new HashMap<>();
        for (PresentasiTerjadwalDTO p : fallbackPresentasiById.values()) {
            scheduledByGroup.put(p.idKelompok(), p);
        }

        List<KelompokPenjadwalanDTO> baseGroups = new ArrayList<>();
        if (Integer.valueOf(101).equals(idKelas)) {
            baseGroups.add(buildFallbackGroup(
                11, 1, "Visualisasi Struktur Data Pohon Berimbang", 4, 30,
                List.of(
                    new AnggotaItemDTO("6182201001", "Dimas Pratama"),
                    new AnggotaItemDTO("6182201002", "Evelyn Wijaya"),
                    new AnggotaItemDTO("6182201003", "Farhan Akbar")
                ),
                scheduledByGroup.get(11)
            ));
            baseGroups.add(buildFallbackGroup(
                12, 2, "Simulasi Algoritma Pencarian Jalur Terpendek", 4, 30,
                List.of(
                    new AnggotaItemDTO("6182201004", "Gita Permata"),
                    new AnggotaItemDTO("6182201005", "Hendra Kusuma"),
                    new AnggotaItemDTO("6182201006", "Indah Lestari"),
                    new AnggotaItemDTO("6182201007", "Joko Susilo")
                ),
                scheduledByGroup.get(12)
            ));
        } else {
            baseGroups.add(buildFallbackGroup(
                1, 1, "Sistem Pendeteksi Intrusi Jaringan", 4, 30,
                List.of(
                    new AnggotaItemDTO("6182101041", "Budi"),
                    new AnggotaItemDTO("6182101044", "Citra"),
                    new AnggotaItemDTO("6182101034", "Andi")
                ),
                scheduledByGroup.get(1)
            ));
            baseGroups.add(buildFallbackGroup(
                2, 2, "Perancangan Topologi Jaringan Kampus", 4, 30,
                List.of(
                    new AnggotaItemDTO("6182101001", "Andi Saputra"),
                    new AnggotaItemDTO("6182101002", "Nurul Hidayah"),
                    new AnggotaItemDTO("6182101003", "Rizky Maulana"),
                    new AnggotaItemDTO("6182101004", "Salsabila Putri")
                ),
                scheduledByGroup.get(2)
            ));
            baseGroups.add(buildFallbackGroup(
                3, 3, "Analisis Kinerja Protokol Routing OSPF", 5, 30,
                List.of(
                    new AnggotaItemDTO("6182101015", "Hana"),
                    new AnggotaItemDTO("6182101016", "Iqbal")
                ),
                scheduledByGroup.get(3)
            ));
        }
        return baseGroups;
    }

    private KelompokPenjadwalanDTO buildFallbackGroup(
        Integer idKelompok,
        Integer nomor,
        String topik,
        Integer kapasitas,
        Integer durasi,
        List<AnggotaItemDTO> rawAnggota,
        PresentasiTerjadwalDTO pres
    ) {
        List<AnggotaPenjadwalanDTO> anggota = toAnggotaPenjadwalanList(rawAnggota);
        if (pres != null) {
            LocalDate tgl = parseDateOrDefault(pres.tanggal(), DEFAULT_REGULER_MONDAY);
            return new KelompokPenjadwalanDTO(
                idKelompok,
                nomor,
                nomor,
                topik,
                kapasitas,
                pres.durasiMenit() != null ? pres.durasiMenit() : durasi,
                true,
                pres.idPresentasi(),
                resolveHariIndonesia(tgl.getDayOfWeek().getValue()),
                pres.tanggal(),
                pres.jamMulai(),
                pres.jamSelesai(),
                pres.ruangan(),
                pres.catatan(),
                anggota
            );
        }
        return new KelompokPenjadwalanDTO(
            idKelompok,
            nomor,
            nomor,
            topik,
            kapasitas,
            durasi,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            anggota
        );
    }

    private MatriksMingguanResponseDTO buildFallbackMatriksMingguan(
        Integer idKelas,
        Integer idKelompok,
        String mode,
        LocalDate monday,
        LocalDate friday,
        String rentangLabel,
        String prevMonday,
        String nextMonday,
        List<HariMingguanDTO> hariList
    ) {
        List<KelompokPenjadwalanDTO> groups = getFallbackKelompokByKelas(idKelas);
        KelompokPenjadwalanDTO selected = groups.get(0);
        if (idKelompok != null) {
            for (KelompokPenjadwalanDTO g : groups) {
                if (idKelompok.equals(g.idKelompok())) {
                    selected = g;
                    break;
                }
            }
        }

        boolean isAsd = Integer.valueOf(101).equals(idKelas);
        String idMatkul = isAsd ? "IF2110" : "IF3130";
        String namaKelas = isAsd
            ? "IF2110 - Algoritma dan Struktur Data (Kelas A)"
            : "IF3130 - Jaringan Komputer (Kelas B)";
        String namaSingkat = isAsd ? "ASD" : "Jarkom";
        String ruanganDefault = isAsd ? "Lab Komputer 1" : "Ruang 9122";

        RawCell[][] grid = new RawCell[11][5];
        for (int h = 0; h < 11; h++) {
            for (int d = 0; d < 5; d++) {
                grid[h][d] = new RawCell("KOSONG", "Kosong", "", "", "", null);
            }
        }

        if ("REGULER".equals(mode)) {
            // Senin 08:00 - 10:00 (h=1,2, d=0): Mahasiswa Sibuk - Andi, Rizky, Salsabila (Kuliah ASD)
            RawCell mhsAsd = new RawCell(
                "BENTROK_MAHASISWA",
                "Mahasiswa Sibuk",
                "Andi, Rizky, Salsabila (Kuliah ASD)",
                "Andi, Rizky, Salsabila",
                "Kuliah ASD",
                null
            );
            grid[1][0] = mhsAsd;
            grid[2][0] = mhsAsd;

            // Selasa 10:00 - 12:00 (h=3,4, d=1): Dosen Sibuk - Mengajar Kelas Lain
            RawCell dosenMengajar = new RawCell(
                "BENTROK_DOSEN",
                "Dosen Sibuk",
                "Mengajar Kelas Lain",
                "",
                "",
                null
            );
            grid[3][1] = dosenMengajar;
            grid[4][1] = dosenMengajar;

            // Rabu 13:00 - 15:00 (h=6,7, d=2): Prioritas Kelas - Jam Reguler Jarkom (Aman, Semua Bisa Hadir)
            RawCell prioritasReguler = new RawCell(
                "PRIORITAS_KELAS",
                "Jam Reguler " + namaSingkat,
                "Aman, Semua Bisa Hadir",
                "",
                "",
                null
            );
            grid[6][2] = prioritasReguler;
            grid[7][2] = prioritasReguler;

            // Kamis 09:00 - 11:00 (h=2,3, d=3): Mahasiswa Sibuk - Nurul, Rizky (Kuliah Sisop)
            RawCell mhsSisop = new RawCell(
                "BENTROK_MAHASISWA",
                "Mahasiswa Sibuk",
                "Nurul, Rizky (Kuliah Sisop)",
                "Nurul, Rizky",
                "Kuliah Sisop",
                null
            );
            grid[2][3] = mhsSisop;
            grid[3][3] = mhsSisop;

            // Jumat 13:00 - 15:00 (h=6,7, d=4): Mahasiswa Sibuk - Andi, Salsabila (Kuliah PBO)
            RawCell mhsPbo = new RawCell(
                "BENTROK_MAHASISWA",
                "Mahasiswa Sibuk",
                "Andi, Salsabila (Kuliah PBO)",
                "Andi, Salsabila",
                "Kuliah PBO",
                null
            );
            grid[6][4] = mhsPbo;
            grid[7][4] = mhsPbo;
        } else if ("UTS".equals(mode)) {
            // Senin 08:00 - 10:00 (h=1,2, d=0): Mahasiswa Sibuk - Nurul (Ujian UTS Back End)
            RawCell mhsUtsBackend = new RawCell(
                "BENTROK_MAHASISWA",
                "Mahasiswa Sibuk",
                "Nurul (Ujian UTS Back End)",
                "Nurul",
                "Ujian UTS Back End",
                null
            );
            grid[1][0] = mhsUtsBackend;
            grid[2][0] = mhsUtsBackend;

            // Selasa 13:00 - 15:00 (h=6,7, d=1): Dosen Sibuk - Mengawas Ujian Mpro
            RawCell dosenMengawasMpro = new RawCell(
                "BENTROK_DOSEN",
                "Dosen Sibuk",
                "Mengawas Ujian Mpro",
                "",
                "",
                null
            );
            grid[6][1] = dosenMengawasMpro;
            grid[7][1] = dosenMengawasMpro;

            // Rabu 10:00 - 12:00 (h=3,4, d=2): Prioritas Kelas - Jadwal Ujian Jarkom (Aman, Semua Bisa Hadir)
            RawCell prioritasUts = new RawCell(
                "PRIORITAS_KELAS",
                "Jadwal Ujian " + namaSingkat,
                "Aman, Semua Bisa Hadir",
                "",
                "",
                null
            );
            grid[3][2] = prioritasUts;
            grid[4][2] = prioritasUts;

            // Kamis 08:00 - 10:00 (h=1,2, d=3): Mahasiswa Sibuk - Andi, Rizky, Salsabila (Ujian UTS ASD)
            RawCell mhsUtsAsd = new RawCell(
                "BENTROK_MAHASISWA",
                "Mahasiswa Sibuk",
                "Andi, Rizky, Salsabila (Ujian UTS ASD)",
                "Andi, Rizky, Salsabila",
                "Ujian UTS ASD",
                null
            );
            grid[1][3] = mhsUtsAsd;
            grid[2][3] = mhsUtsAsd;
        } else {
            // UAS Mode
            // Senin 10:00 - 12:00 (h=3,4, d=0): Dosen Sibuk - Mengawas Ujian Mpro
            RawCell dosenUasMpro = new RawCell(
                "BENTROK_DOSEN",
                "Dosen Sibuk",
                "Mengawas Ujian Mpro",
                "",
                "",
                null
            );
            grid[3][0] = dosenUasMpro;
            grid[4][0] = dosenUasMpro;

            // Selasa 08:00 - 10:00 (h=1,2, d=1): Mahasiswa Sibuk - Andi, Rizky (Ujian UAS Basdat)
            RawCell mhsUasBasdat = new RawCell(
                "BENTROK_MAHASISWA",
                "Mahasiswa Sibuk",
                "Andi, Rizky (Ujian UAS Basdat)",
                "Andi, Rizky",
                "Ujian UAS Basdat",
                null
            );
            grid[1][1] = mhsUasBasdat;
            grid[2][1] = mhsUasBasdat;

            // Kamis 13:00 - 15:00 (h=6,7, d=3): Prioritas Kelas - Jadwal Ujian Jarkom (Aman, Semua Bisa Hadir)
            RawCell prioritasUas = new RawCell(
                "PRIORITAS_KELAS",
                "Jadwal Ujian " + namaSingkat,
                "Aman, Semua Bisa Hadir",
                "",
                "",
                null
            );
            grid[6][3] = prioritasUas;
            grid[7][3] = prioritasUas;

            // Jumat 09:00 - 11:00 (h=2,3, d=4): Mahasiswa Sibuk - Nurul, Salsabila (Ujian UAS Back End)
            RawCell mhsUasBackend = new RawCell(
                "BENTROK_MAHASISWA",
                "Mahasiswa Sibuk",
                "Nurul, Salsabila (Ujian UAS Back End)",
                "Nurul, Salsabila",
                "Ujian UAS Back End",
                null
            );
            grid[2][4] = mhsUasBackend;
            grid[3][4] = mhsUasBackend;
        }

        // Overlay existing scheduled presentations from fallbackPresentasiById
        for (int d = 0; d < 5; d++) {
            LocalDate date = monday.plusDays(d);
            for (int h = 0; h < 11; h++) {
                LocalTime slotStart = LocalTime.of(7 + h, 0);
                LocalTime slotEnd = LocalTime.of(8 + h, 0);

                List<String> bookedIntervals = new ArrayList<>();
                PresentasiTerjadwalDTO own = null;
                PresentasiTerjadwalDTO other = null;

                for (PresentasiTerjadwalDTO p : fallbackPresentasiById.values()) {
                    if (p.tanggal() != null && p.tanggal().equals(date.toString())) {
                        LocalTime pStart = parseTimeOrDefault(p.jamMulai(), LocalTime.of(13, 0));
                        LocalTime pEnd = parseTimeOrDefault(p.jamSelesai(), LocalTime.of(13, 30));
                        if (isOverlapping(slotStart, slotEnd, pStart, pEnd)) {
                            String pRange = pStart.format(TIME_FMT) + " - " + pEnd.format(TIME_FMT);
                            if (!bookedIntervals.contains(pRange)) {
                                bookedIntervals.add(pRange);
                            }
                            if (selected != null && selected.idKelompok() != null && selected.idKelompok().equals(p.idKelompok())) {
                                own = p;
                            } else {
                                other = p;
                            }
                        }
                    }
                }

                if (own != null) {
                    String ownMatkul = (own.namaMatkul() != null && !own.namaMatkul().isBlank())
                        ? own.namaMatkul()
                        : (isAsd ? "Algoritma dan Struktur Data" : "Jaringan Komputer");
                    String dotStart = own.jamMulai() != null ? own.jamMulai().replace(':', '.') : "09.00";
                    String dotEnd = own.jamSelesai() != null ? own.jamSelesai().replace(':', '.') : "09.30";
                    grid[h][d] = new RawCell(
                        "TERJADWAL",
                        "Terjadwal",
                        "Kelompok " + own.nomorKelompok() + "\n" + ownMatkul + "\n" + dotStart + " - " + dotEnd,
                        "",
                        "",
                        own.idPresentasi(),
                        bookedIntervals
                    );
                } else if (other != null) {
                    String otherMatkul = (other.namaMatkul() != null && !other.namaMatkul().isBlank())
                        ? other.namaMatkul()
                        : (isAsd ? "Algoritma dan Struktur Data" : "Jaringan Komputer");
                    String dotStart = other.jamMulai() != null ? other.jamMulai().replace(':', '.') : "09.00";
                    String dotEnd = other.jamSelesai() != null ? other.jamSelesai().replace(':', '.') : "09.30";
                    grid[h][d] = new RawCell(
                        "BENTROK_DOSEN",
                        "Dosen Sibuk",
                        "Menguji Kelompok " + other.nomorKelompok() + "\n" + otherMatkul + "\n" + dotStart + " - " + dotEnd,
                        "",
                        "",
                        other.idPresentasi(),
                        bookedIntervals
                    );
                } else if (!bookedIntervals.isEmpty()) {
                    RawCell existing = grid[h][d];
                    grid[h][d] = new RawCell(
                        existing.status(),
                        existing.badgeText(),
                        existing.deskripsi(),
                        existing.daftarMahasiswaBentrok(),
                        existing.namaAgendaBentrok(),
                        existing.idPresentasiTerjadwal(),
                        bookedIntervals
                    );
                }
            }
        }

        List<BarisMatriksMingguanDTO> barisList = buildMergedRowsWithRowSpan(grid, hariList);

        return new MatriksMingguanResponseDTO(
            idKelas,
            namaKelas,
            idMatkul,
            namaSingkat,
            ruanganDefault,
            selected.idKelompok(),
            selected.noKelompok(),
            selected.durasiMenit(),
            mode,
            monday.toString(),
            friday.toString(),
            rentangLabel,
            prevMonday,
            nextMonday,
            hariList,
            barisList
        );
    }

    private MatriksPenjadwalanResponseDTO buildFallbackMatriks(
        Integer idKelas,
        String ruangan,
        LocalDate tanggal,
        Integer idKelompokFilter
    ) {
        String namaKelas = Integer.valueOf(101).equals(idKelas)
            ? "IF2110 - Algoritma dan Struktur Data (Kelas A)"
            : "IF3130 - Jaringan Komputer (Kelas B)";

        List<SlotMatriksDTO> slots = new ArrayList<>();
        for (int hour = 7; hour < 17; hour++) {
            LocalTime slotStart = LocalTime.of(hour, 0);
            LocalTime slotEnd = LocalTime.of(hour + 1, 0);
            String startStr = slotStart.format(TIME_FMT);
            String endStr = slotEnd.format(TIME_FMT);
            String label = startStr + " - " + endStr;

            PresentasiTerjadwalDTO booked = findFallbackPresentasiInSlot(ruangan, tanggal, slotStart, slotEnd);
            if (booked != null) {
                slots.add(new SlotMatriksDTO(
                    startStr, endStr, label, "TERISI", null, Collections.emptyList(), booked
                ));
                continue;
            }

            List<KonflikDetailDTO> conflicts = evaluateFallbackConflicts(
                idKelas, idKelompokFilter, ruangan, tanggal, slotStart, slotEnd, false
            );
            if (!conflicts.isEmpty()) {
                slots.add(new SlotMatriksDTO(
                    startStr, endStr, label, "BENTROK", conflicts.get(0).deskripsi(), conflicts, null
                ));
            } else {
                slots.add(new SlotMatriksDTO(
                    startStr, endStr, label, "TERSEDIA", null, Collections.emptyList(), null
                ));
            }
        }

        return new MatriksPenjadwalanResponseDTO(
            idKelas,
            namaKelas,
            ruangan,
            tanggal.toString(),
            resolveHariIndonesia(tanggal.getDayOfWeek().getValue()),
            slots
        );
    }

    private PresentasiTerjadwalDTO findFallbackPresentasiInSlot(
        String ruangan,
        LocalDate tanggal,
        LocalTime slotStart,
        LocalTime slotEnd
    ) {
        for (PresentasiTerjadwalDTO p : fallbackPresentasiById.values()) {
            if (tanggal.toString().equals(p.tanggal()) && ruangan.equalsIgnoreCase(p.ruangan())) {
                LocalTime pStart = parseTimeOrDefault(p.jamMulai(), LocalTime.of(13, 0));
                LocalTime pEnd = parseTimeOrDefault(p.jamSelesai(), LocalTime.of(13, 30));
                if (isOverlapping(slotStart, slotEnd, pStart, pEnd)) {
                    return p;
                }
            }
        }
        return null;
    }

    private CekBentrokResponseDTO cekBentrokFallback(
        Integer idKelas,
        Integer idKelompok,
        String ruangan,
        LocalDate tanggal,
        LocalTime jamMulai,
        LocalTime jamSelesai,
        int durasi
    ) {
        List<KonflikDetailDTO> conflicts = evaluateFallbackConflicts(
            idKelas, idKelompok, ruangan, tanggal, jamMulai, jamSelesai, true
        );
        return new CekBentrokResponseDTO(
            !conflicts.isEmpty(),
            jamMulai.format(TIME_FMT),
            jamSelesai.format(TIME_FMT),
            durasi,
            conflicts
        );
    }

    private List<KonflikDetailDTO> evaluateFallbackConflicts(
        Integer idKelas,
        Integer idKelompok,
        String ruangan,
        LocalDate tanggal,
        LocalTime jamMulai,
        LocalTime jamSelesai,
        boolean checkKelompokAlreadyScheduled
    ) {
        List<KonflikDetailDTO> conflicts = new ArrayList<>();

        if (checkKelompokAlreadyScheduled && idKelompok != null) {
            for (PresentasiTerjadwalDTO p : fallbackPresentasiById.values()) {
                if (idKelompok.equals(p.idKelompok())) {
                    conflicts.add(new KonflikDetailDTO(
                        "KELOMPOK",
                        "Bentrok Kelompok",
                        String.format(
                            "Bentrok Kelompok: Kelompok %d sudah memiliki jadwal presentasi pada %s (%s - %s di %s)",
                            p.nomorKelompok(), p.tanggal(), p.jamMulai(), p.jamSelesai(), p.ruangan()
                        )
                    ));
                }
            }
        }

        for (PresentasiTerjadwalDTO p : fallbackPresentasiById.values()) {
            if (idKelompok != null && idKelompok.equals(p.idKelompok())) {
                continue;
            }
            if (tanggal.toString().equals(p.tanggal())) {
                LocalTime pStart = parseTimeOrDefault(p.jamMulai(), LocalTime.of(13, 0));
                LocalTime pEnd = parseTimeOrDefault(p.jamSelesai(), LocalTime.of(13, 30));
                if (isOverlapping(jamMulai, jamSelesai, pStart, pEnd)) {
                    if (ruangan.equalsIgnoreCase(p.ruangan())) {
                        conflicts.add(new KonflikDetailDTO(
                            "RUANGAN",
                            "Bentrok Ruangan",
                            String.format(
                                "Bentrok Ruangan: %s telah digunakan presentasi Kelompok %d (%s - %s)",
                                ruangan, p.nomorKelompok(), p.jamMulai(), p.jamSelesai()
                            )
                        ));
                    }
                    conflicts.add(new KonflikDetailDTO(
                        "DOSEN",
                        "Bentrok Dosen",
                        String.format(
                            "Bentrok Dosen: Dr. Budi Santoso menguji presentasi Kelompok %d (%s - %s)",
                            p.nomorKelompok(), p.jamMulai(), p.jamSelesai()
                        )
                    ));
                }
            }
        }

        int dow = tanggal.getDayOfWeek().getValue();
        // Match the weekly fallback schedule conflicts:
        if (dow == 1 && isOverlapping(jamMulai, jamSelesai, LocalTime.of(8, 0), LocalTime.of(10, 0))) {
            conflicts.add(new KonflikDetailDTO(
                "MAHASISWA",
                "Bentrok Mahasiswa",
                "Bentrok Mahasiswa: Andi, Rizky, Salsabila memiliki jadwal Kuliah ASD (08:00 - 10:00)"
            ));
        }
        if (dow == 2 && isOverlapping(jamMulai, jamSelesai, LocalTime.of(10, 0), LocalTime.of(12, 0))) {
            conflicts.add(new KonflikDetailDTO(
                "DOSEN",
                "Bentrok Dosen",
                "Bentrok Dosen: Dr. Budi Santoso memiliki jadwal Mengajar Kelas Lain (10:00 - 12:00)"
            ));
        }
        if (dow == 4 && isOverlapping(jamMulai, jamSelesai, LocalTime.of(9, 0), LocalTime.of(11, 0))) {
            conflicts.add(new KonflikDetailDTO(
                "MAHASISWA",
                "Bentrok Mahasiswa",
                "Bentrok Mahasiswa: Nurul, Rizky memiliki jadwal Kuliah Sisop (09:00 - 11:00)"
            ));
        }
        if (dow == 5 && isOverlapping(jamMulai, jamSelesai, LocalTime.of(13, 0), LocalTime.of(15, 0))) {
            conflicts.add(new KonflikDetailDTO(
                "MAHASISWA",
                "Bentrok Mahasiswa",
                "Bentrok Mahasiswa: Andi, Salsabila memiliki jadwal Kuliah PBO (13:00 - 15:00)"
            ));
        }

        return conflicts;
    }

    private PresentasiTerjadwalDTO simpanFallbackPresentasi(
        Integer idKelas,
        Integer idKelompok,
        String ruangan,
        LocalDate tanggal,
        LocalTime jamMulai,
        LocalTime jamSelesai,
        int durasi,
        String catatan
    ) {
        List<KelompokPenjadwalanDTO> groups = getFallbackKelompokByKelas(idKelas);
        KelompokPenjadwalanDTO target = groups.stream()
            .filter(g -> idKelompok.equals(g.idKelompok()))
            .findFirst()
            .orElse(new KelompokPenjadwalanDTO(
                idKelompok, 1, 1, "Kelompok " + idKelompok, 4, durasi, false,
                null, null, null, null, null, null, null, Collections.emptyList()
            ));

        int newId = fallbackIdSequence.incrementAndGet();
        String kodeKelas = Integer.valueOf(101).equals(idKelas) ? "A" : "B";
        String namaMatkul = Integer.valueOf(101).equals(idKelas)
            ? "Algoritma dan Struktur Data"
            : "Jaringan Komputer";

        List<AnggotaItemDTO> anggotaItems = new ArrayList<>();
        if (target.anggota() != null) {
            for (AnggotaPenjadwalanDTO a : target.anggota()) {
                anggotaItems.add(new AnggotaItemDTO(a.npm(), a.nama()));
            }
        }

        PresentasiTerjadwalDTO created = new PresentasiTerjadwalDTO(
            newId,
            target.idKelompok(),
            target.nomorKelompok(),
            target.topik(),
            tanggal.toString(),
            jamMulai.format(TIME_FMT),
            jamSelesai.format(TIME_FMT),
            ruangan,
            durasi,
            catatan != null && !catatan.isBlank() ? catatan.trim() : null,
            kodeKelas,
            namaMatkul,
            anggotaItems
        );
        fallbackPresentasiById.put(newId, created);
        return created;
    }

    private record RawCell(
        String status,
        String badgeText,
        String deskripsi,
        String daftarMahasiswaBentrok,
        String namaAgendaBentrok,
        Integer idPresentasiTerjadwal,
        List<String> intervalTerpakai
    ) {
        public RawCell(
            String status,
            String badgeText,
            String deskripsi,
            String daftarMahasiswaBentrok,
            String namaAgendaBentrok,
            Integer idPresentasiTerjadwal
        ) {
            this(
                status,
                badgeText,
                deskripsi,
                daftarMahasiswaBentrok,
                namaAgendaBentrok,
                idPresentasiTerjadwal,
                Collections.emptyList()
            );
        }
    }

    private record WeeklyBatchContext(
        Set<String> dosenIds,
        List<Kuliah> kuliahKelasIni,
        List<Kuliah> kuliahDosenAllDays,
        List<PengawasUjian> pengawasDosenWeek,
        List<Presentasi> presentasiWeekAll,
        List<PesertaKelas> pesertaMahasiswa,
        List<Kuliah> kuliahMahasiswaAllDays,
        List<Ujian> ujianMahasiswaWeek,
        List<AnggotaKelompok> anggotaSemuaKelasMahasiswa,
        Map<Integer, Presentasi> presentasiByKelompokId
    ) {
    }

    private record BatchConflictContext(
        Integer idKelas,
        Set<String> dosenIds,
        Map<Integer, Kelompok> kelompokById,
        Map<Integer, List<AnggotaItemDTO>> anggotaByKelompokId,
        List<Kuliah> kuliahDosen,
        List<PengawasUjian> pengawasDosen,
        List<Presentasi> presentasiTanggalAll,
        List<PesertaKelas> pesertaMahasiswa,
        List<Kuliah> kuliahMahasiswa,
        List<Ujian> ujianMahasiswa,
        List<AnggotaKelompok> anggotaSemuaKelasMahasiswa,
        List<Presentasi> presentasiMahasiswa,
        List<Kuliah> kuliahRuangan,
        List<Ujian> ujianRuangan,
        List<Presentasi> presentasiRuangan,
        Map<Integer, Presentasi> presentasiByKelompokId
    ) {
    }
}
