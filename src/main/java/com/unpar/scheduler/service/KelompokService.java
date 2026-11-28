package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.kelompok.AnggotaItemDTO;
import com.unpar.scheduler.dto.kelompok.BatchCreateKelompokRequestDTO;
import com.unpar.scheduler.dto.kelompok.CreateKelompokRequestDTO;
import com.unpar.scheduler.dto.kelompok.KelompokDetailDTO;
import com.unpar.scheduler.dto.kelompok.UpdateKelompokRequestDTO;
import com.unpar.scheduler.model.entity.AnggotaKelompok;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kelompok;
import com.unpar.scheduler.repository.AnggotaKelompokRepository;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KelompokRepository;
import com.unpar.scheduler.repository.PresentasiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class KelompokService {

    private final KelompokRepository kelompokRepository;
    private final AnggotaKelompokRepository anggotaKelompokRepository;
    private final KelasRepository kelasRepository;
    private final PresentasiRepository presentasiRepository;

    public KelompokService(
        KelompokRepository kelompokRepository,
        AnggotaKelompokRepository anggotaKelompokRepository,
        KelasRepository kelasRepository,
        PresentasiRepository presentasiRepository
    ) {
        this.kelompokRepository = kelompokRepository;
        this.anggotaKelompokRepository = anggotaKelompokRepository;
        this.kelasRepository = kelasRepository;
        this.presentasiRepository = presentasiRepository;
    }

    @Transactional(readOnly = true)
    public List<KelompokDetailDTO> getKelompokByKelas(Integer idKelas) {
        if (idKelas == null) {
            return Collections.emptyList();
        }

        List<Kelompok> kelompokList = kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(idKelas);
        if (kelompokList.isEmpty()) {
            return Collections.emptyList();
        }

        List<KelompokDetailDTO> result = new ArrayList<>();
        for (Kelompok k : kelompokList) {
            List<AnggotaItemDTO> anggotaDTOs = mapAnggota(anggotaKelompokRepository.findByKelompok_IdKelompok(k.getIdKelompok()));
            result.add(toKelompokDetailDTO(k, anggotaDTOs));
        }

        return result;
    }

    public KelompokDetailDTO createKelompok(CreateKelompokRequestDTO request) {
        if (request == null || request.idKelas() == null) {
            throw new IllegalArgumentException("Data kelas dan kelompok tidak valid");
        }
        int durasi = validateDurasiMenit(request.durasiMenit());

        Kelas kelas = kelasRepository.findById(request.idKelas())
            .orElseThrow(() -> new IllegalArgumentException("Kelas dengan ID " + request.idKelas() + " tidak ditemukan"));

        Kelompok kelompok = new Kelompok();
        kelompok.setKelas(kelas);
        kelompok.setNoKelompok(request.nomorKelompok() != null ? request.nomorKelompok().shortValue() : (short) 1);
        kelompok.setKapasitas(request.batasKapasitas() != null ? request.batasKapasitas().shortValue() : (short) 4);
        kelompok.setDurasiPresentasi((short) durasi);
        kelompok.setTopik(request.topik());

        Kelompok saved = kelompokRepository.save(kelompok);
        return toKelompokDetailDTO(saved, new ArrayList<>());
    }

    public List<KelompokDetailDTO> batchCreateKelompok(BatchCreateKelompokRequestDTO request) {
        if (request == null || request.idKelas() == null) {
            throw new IllegalArgumentException("Data kelas tidak valid");
        }

        int jumlah = (request.jumlahKelompok() != null && request.jumlahKelompok() > 0) ? request.jumlahKelompok() : 1;
        int kapasitas = (request.batasKapasitas() != null && request.batasKapasitas() > 0) ? request.batasKapasitas() : 5;
        int durasi = validateDurasiMenit(request.durasiMenit());

        Kelas kelas = kelasRepository.findById(request.idKelas())
            .orElseThrow(() -> new IllegalArgumentException("Kelas dengan ID " + request.idKelas() + " tidak ditemukan"));

        List<Kelompok> existingKelompok = kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(request.idKelas());
        int currentMax = existingKelompok.stream()
            .mapToInt(k -> k.getNoKelompok() != null ? k.getNoKelompok().intValue() : 0)
            .max()
            .orElse(0);

        List<Kelompok> newEntities = new ArrayList<>();
        boolean hasCustomTopik = (request.defaultTopik() != null && !request.defaultTopik().isBlank());
        String customTopik = hasCustomTopik ? request.defaultTopik().trim() : null;

        for (int i = 1; i <= jumlah; i++) {
            int nomorBaru = currentMax + i;
            String topik = hasCustomTopik ? customTopik : ("Kelompok " + nomorBaru);

            Kelompok kelompok = new Kelompok();
            kelompok.setKelas(kelas);
            kelompok.setNoKelompok((short) nomorBaru);
            kelompok.setKapasitas((short) kapasitas);
            kelompok.setDurasiPresentasi((short) durasi);
            kelompok.setTopik(topik);
            newEntities.add(kelompok);
        }

        kelompokRepository.saveAll(newEntities);

        return getKelompokByKelas(request.idKelas());
    }

    public KelompokDetailDTO updateKelompok(Integer idKelompok, UpdateKelompokRequestDTO request) {
        if (idKelompok == null) {
            throw new IllegalArgumentException("ID kelompok tidak valid");
        }

        Kelompok kelompok = kelompokRepository.findById(idKelompok)
            .orElseThrow(() -> new IllegalArgumentException("Kelompok dengan ID " + idKelompok + " tidak ditemukan"));

        if (request != null) {
            if (request.topik() != null && !request.topik().isBlank()) {
                kelompok.setTopik(request.topik().trim());
            }
            if (request.batasKapasitas() != null && request.batasKapasitas() > 0) {
                kelompok.setKapasitas(request.batasKapasitas().shortValue());
            }
            if (request.durasiMenit() != null) {
                kelompok.setDurasiPresentasi((short) validateDurasiMenit(request.durasiMenit()));
            }
        }
        if (kelompok.getDurasiPresentasi() == null || kelompok.getDurasiPresentasi() < 5) {
            throw new IllegalArgumentException("Durasi presentasi minimal 5 menit");
        }

        Kelompok saved = kelompokRepository.save(kelompok);
        List<AnggotaItemDTO> anggotaDTOs = mapAnggota(anggotaKelompokRepository.findByKelompok_IdKelompok(saved.getIdKelompok()));
        return toKelompokDetailDTO(saved, anggotaDTOs);
    }

    private int validateDurasiMenit(Integer durasiMenit) {
        if (durasiMenit == null || durasiMenit < 5) {
            throw new IllegalArgumentException("Durasi presentasi minimal 5 menit");
        }
        return durasiMenit;
    }

    private List<AnggotaItemDTO> mapAnggota(List<AnggotaKelompok> anggotaList) {
        List<AnggotaItemDTO> anggotaDTOs = new ArrayList<>();
        for (AnggotaKelompok ak : anggotaList) {
            if (ak.getMahasiswa() != null) {
                anggotaDTOs.add(new AnggotaItemDTO(
                    ak.getMahasiswa().getNpm(),
                    ak.getMahasiswa().getNama()
                ));
            }
        }
        return anggotaDTOs;
    }

    private KelompokDetailDTO toKelompokDetailDTO(Kelompok k, List<AnggotaItemDTO> anggotaDTOs) {
        int currentCount = anggotaDTOs.size();
        int maxCap = k.getKapasitas() != null ? k.getKapasitas().intValue() : 0;
        String status = (currentCount >= maxCap && maxCap > 0) ? "Lengkap" : "Menunggu Mahasiswa";
        Integer durasi = k.getDurasiPresentasi() != null ? k.getDurasiPresentasi().intValue() : null;

        return new KelompokDetailDTO(
            k.getIdKelompok(),
            k.getNoKelompok() != null ? k.getNoKelompok().intValue() : 1,
            k.getTopik() != null ? k.getTopik() : "-",
            maxCap,
            durasi,
            status,
            anggotaDTOs
        );
    }

    @Transactional
    public void deleteKelompok(Integer idKelompok) {
        if (idKelompok == null) {
            throw new IllegalArgumentException("ID kelompok tidak valid");
        }

        Kelompok target = kelompokRepository.findById(idKelompok)
                .orElseThrow(() -> new IllegalArgumentException("Kelompok tidak ditemukan dengan ID: " + idKelompok));

        // 1. Hapus presentasi terkait (jika ada)
        presentasiRepository.deleteByKelompokId(idKelompok);

        // 2. Hapus relasi anggota terlebih dahulu
        anggotaKelompokRepository.deleteByKelompok_IdKelompok(idKelompok);

        // 3. Hapus entitas kelompok
        kelompokRepository.delete(target);
    }
}
