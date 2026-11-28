package com.unpar.scheduler.service;

import com.unpar.scheduler.model.entity.Dosen;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kuliah;
import com.unpar.scheduler.repository.KuliahRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Menentukan nama dosen tiap kelas: penanggung jawab kelas, lalu dosen pengajar kuliah, lalu koordinator mata kuliah.
@Component
public class NamaDosenPengajarResolver {

    static final String TANPA_DOSEN = "-";

    private final KuliahRepository kuliahRepository;

    public NamaDosenPengajarResolver(KuliahRepository kuliahRepository) {
        this.kuliahRepository = kuliahRepository;
    }

    public Map<Integer, String> resolve(Collection<Kelas> daftarKelas) {
        Map<Integer, String> namaPerKelas = new HashMap<>();
        List<Kelas> kelasTanpaPenanggungJawab = new ArrayList<>();

        for (Kelas kelas : daftarKelas) {
            if (hasNama(kelas.getPenanggungJawab())) {
                namaPerKelas.put(kelas.getIdKelas(), kelas.getPenanggungJawab().getNama().trim());
            } else {
                kelasTanpaPenanggungJawab.add(kelas);
            }
        }
        if (kelasTanpaPenanggungJawab.isEmpty()) {
            return namaPerKelas;
        }

        Map<Integer, Set<String>> pengajarPerKelas = cariPengajarKuliah(kelasTanpaPenanggungJawab);
        for (Kelas kelas : kelasTanpaPenanggungJawab) {
            Set<String> pengajar = pengajarPerKelas.get(kelas.getIdKelas());
            if (pengajar != null && !pengajar.isEmpty()) {
                namaPerKelas.put(kelas.getIdKelas(), String.join(", ", pengajar));
            } else if (hasNama(kelas.getMataKuliahBuka().getKoordinator())) {
                namaPerKelas.put(kelas.getIdKelas(), kelas.getMataKuliahBuka().getKoordinator().getNama().trim());
            } else {
                namaPerKelas.put(kelas.getIdKelas(), TANPA_DOSEN);
            }
        }
        return namaPerKelas;
    }

    private static boolean hasNama(Dosen dosen) {
        return dosen != null && dosen.getNama() != null && !dosen.getNama().isBlank();
    }

    private Map<Integer, Set<String>> cariPengajarKuliah(List<Kelas> daftarKelas) {
        List<Integer> idKelas = daftarKelas.stream().map(Kelas::getIdKelas).toList();
        Map<Integer, Set<String>> hasil = new HashMap<>();
        for (Kuliah kuliah : kuliahRepository.findByKelasIdsWithDetails(idKelas)) {
            // Set menghindari nama ganda untuk kelas yang punya kuliah teori dan praktikum
            hasil.computeIfAbsent(kuliah.getKelas().getIdKelas(), key -> new LinkedHashSet<>())
                .add(kuliah.getDosenPengajar().getNama());
        }
        return hasil;
    }
}
