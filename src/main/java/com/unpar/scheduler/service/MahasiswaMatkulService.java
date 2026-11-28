package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.matkul.KelasPilihanDTO;
import com.unpar.scheduler.dto.matkul.MatkulItemDTO;
import com.unpar.scheduler.dto.matkul.MatkulTersediaResponseDTO;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kuliah;
import com.unpar.scheduler.model.entity.MataKuliah;
import com.unpar.scheduler.model.entity.PesertaKelas;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KuliahRepository;
import com.unpar.scheduler.repository.PesertaKelasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

@Service
@Transactional(readOnly = true)

// Menyusun daftar mata kuliah dan kelas semester aktif untuk mahasiswa, termasuk 
// dengan dosen pengajar dan status terkunci (Jika sudah memilih atau belum)
public class MahasiswaMatkulService {

    private final KelasRepository kelasRepository;
    private final KuliahRepository kuliahRepository;
    private final PesertaKelasRepository pesertaKelasRepository;

    public MahasiswaMatkulService(
        KelasRepository kelasRepository,
        KuliahRepository kuliahRepository,
        PesertaKelasRepository pesertaKelasRepository
    ) {
        this.kelasRepository = kelasRepository;
        this.kuliahRepository = kuliahRepository;
        this.pesertaKelasRepository = pesertaKelasRepository;
    }

    // Method menyusun daftar matkul + kelas untuk satu mahasiswa
    public MatkulTersediaResponseDTO getMatkulTersedia(String npm) {
        Optional<String> semesterOpt = kelasRepository.findLatestSemesterOverall();
        if (semesterOpt.isEmpty() || semesterOpt.get().isBlank()) {
            return new MatkulTersediaResponseDTO(null, "-", false, List.of());
        }
        String semester = semesterOpt.get().trim();
        List<Kelas> semuaKelas = kelasRepository.findBySemester(semester);
        Set<Integer> kelasDipilih = cariKelasDipilih(npm, semester);
        Map<Integer, Set<String>> dosenPerKelas = cariDosenPerKelas(semuaKelas);
        Map<String, List<Kelas>> kelasPerMatkul = new TreeMap<>();  // Menggunakan treemap agar matkul tersusun berdasarkan kode
        
        for (Kelas kelas : semuaKelas) {
            String idMatkul = kelas.getMataKuliahBuka().getMatakuliah().getIdMatkul();
            kelasPerMatkul.computeIfAbsent(idMatkul, key -> new ArrayList<>()).add(kelas);
        }

        List<MatkulItemDTO> daftarMatkul = new ArrayList<>();
        for (List<Kelas> kelasList : kelasPerMatkul.values()) {
            MataKuliah matkul = kelasList.get(0).getMataKuliahBuka().getMatakuliah();
            List<KelasPilihanDTO> pilihanKelas = new ArrayList<>();
            for (Kelas kelas : kelasList) {
                Set<String> namaDosen = dosenPerKelas.get(kelas.getIdKelas());
                String dosenPengajar = (namaDosen == null || namaDosen.isEmpty()) ? "-" : String.join(", ", namaDosen);

                // Tandai kelas dipilih jika ada di daftar pilihan dari mahasiswa
                pilihanKelas.add(new KelasPilihanDTO(
                    kelas.getIdKelas(),
                    kelas.getKodeKelas(),
                    dosenPengajar,
                    kelasDipilih.contains(kelas.getIdKelas())
                ));
            }
            daftarMatkul.add(new MatkulItemDTO(matkul.getIdMatkul(), matkul.getNama(), matkul.getSks(), pilihanKelas));
        }

        boolean terkunci = !kelasDipilih.isEmpty();
        return new MatkulTersediaResponseDTO(semester, formatSemesterLabel(semester), terkunci, daftarMatkul);
    }

    // Mengembalikan id kelas yang sudah dipilih mahasiswa pada semester ini
    private Set<Integer> cariKelasDipilih(String npm, String semester) {
        Set<Integer> hasil = new HashSet<>();

        for (PesertaKelas peserta : pesertaKelasRepository.findByMahasiswaNpmInWithDetails(List.of(npm))) {
            Kelas kelas = peserta.getKelas();
            if (semester.equals(kelas.getMataKuliahBuka().getSemesterAkademik())) {
                hasil.add(kelas.getIdKelas());
            }
        }
        return hasil;
    }

    // Mengembalikan map: idkelas -> nama dosen yang mengajar kelas itu
    private Map<Integer, Set<String>> cariDosenPerKelas(List<Kelas> semuaKelas) {
        Map<Integer, Set<String>> hasil = new HashMap<>();
        if (semuaKelas.isEmpty()) {
            return hasil;
        }
        // Kumpulkan semua id kelas, lalu ambil jadwal kuliahnya sekaligus (satu query)
        List<Integer> kelasIds = semuaKelas.stream().map(Kelas::getIdKelas).toList();
        for (Kuliah kuliah : kuliahRepository.findByKelasIdsWithDetails(kelasIds)) {
            // Set dipakai agar nama dosen yang sama tidak muncul double (teori + praktikum)
            hasil.computeIfAbsent(kuliah.getKelas().getIdKelas(), key -> new LinkedHashSet<>())
                .add(kuliah.getDosenPengajar().getNama());
        }
        return hasil;
    }

    // Mengubah format kode semester. Contoh: "25261" -> "Semester Ganjil 2025/2026"
    private String formatSemesterLabel(String kode) {
        if (kode.length() == 5 && kode.chars().allMatch(Character::isDigit)) {
            int tahunAwal = 2000 + Integer.parseInt(kode.substring(0, 2));
            int tahunAkhir = 2000 + Integer.parseInt(kode.substring(2, 4));
            
            String jenis = switch (kode.charAt(4)) {
                case '1' -> "Ganjil";
                case '2' -> "Genap";
                case '3' -> "Pendek";
                default -> null;
            };
            if (jenis != null) {
                return "Semester " + jenis + " " + tahunAwal + "/" + tahunAkhir;
            }
        }
        // Format tidak dikenali
        return kode;
    }
}