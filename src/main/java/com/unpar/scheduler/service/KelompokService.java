package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.kelompok.AnggotaItemDTO;
import com.unpar.scheduler.dto.kelompok.CreateKelompokRequestDTO;
import com.unpar.scheduler.dto.kelompok.KelompokDetailDTO;
import com.unpar.scheduler.model.entity.AnggotaKelompok;
import com.unpar.scheduler.model.entity.Kelas;
import com.unpar.scheduler.model.entity.Kelompok;
import com.unpar.scheduler.repository.AnggotaKelompokRepository;
import com.unpar.scheduler.repository.KelasRepository;
import com.unpar.scheduler.repository.KelompokRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class KelompokService {

    private final KelompokRepository kelompokRepository;
    private final AnggotaKelompokRepository anggotaKelompokRepository;
    private final KelasRepository kelasRepository;

    public KelompokService(
        KelompokRepository kelompokRepository,
        AnggotaKelompokRepository anggotaKelompokRepository,
        KelasRepository kelasRepository
    ) {
        this.kelompokRepository = kelompokRepository;
        this.anggotaKelompokRepository = anggotaKelompokRepository;
        this.kelasRepository = kelasRepository;
    }

    @Transactional(readOnly = true)
    public List<KelompokDetailDTO> getKelompokByKelas(Integer idKelas) {
        if (idKelas == null) {
            return buildFallbackKelompokList();
        }

        List<Kelompok> kelompokList = kelompokRepository.findByKelas_IdKelasOrderByNoKelompokAsc(idKelas);
        if (kelompokList.isEmpty()) {
            return buildFallbackKelompokList();
        }

        List<KelompokDetailDTO> result = new ArrayList<>();
        for (Kelompok k : kelompokList) {
            List<AnggotaKelompok> anggotaList = anggotaKelompokRepository.findByKelompok_IdKelompok(k.getIdKelompok());
            List<AnggotaItemDTO> anggotaDTOs = new ArrayList<>();
            for (AnggotaKelompok ak : anggotaList) {
                if (ak.getMahasiswa() != null) {
                    anggotaDTOs.add(new AnggotaItemDTO(
                        ak.getMahasiswa().getNpm(),
                        ak.getMahasiswa().getNama()
                    ));
                }
            }

            int currentCount = anggotaDTOs.size();
            int maxCap = (k.getKapasitas() != null) ? k.getKapasitas().intValue() : 0;
            String status = (currentCount >= maxCap && maxCap > 0) ? "Lengkap" : "Menunggu Mahasiswa";

            result.add(new KelompokDetailDTO(
                k.getIdKelompok(),
                k.getNoKelompok() != null ? k.getNoKelompok().intValue() : 1,
                k.getTopik() != null ? k.getTopik() : "-",
                maxCap,
                30,
                status,
                anggotaDTOs
            ));
        }

        return result;
    }

    public KelompokDetailDTO createKelompok(CreateKelompokRequestDTO request) {
        if (request == null || request.idKelas() == null) {
            throw new IllegalArgumentException("Data kelas dan kelompok tidak valid");
        }

        Kelas kelas = kelasRepository.findById(request.idKelas())
            .orElseThrow(() -> new IllegalArgumentException("Kelas dengan ID " + request.idKelas() + " tidak ditemukan"));

        Kelompok kelompok = new Kelompok();
        kelompok.setKelas(kelas);
        kelompok.setNoKelompok(request.nomorKelompok() != null ? request.nomorKelompok().shortValue() : (short) 1);
        kelompok.setKapasitas(request.batasKapasitas() != null ? request.batasKapasitas().shortValue() : (short) 4);
        kelompok.setTopik(request.topik());

        Kelompok saved = kelompokRepository.save(kelompok);

        return new KelompokDetailDTO(
            saved.getIdKelompok(),
            saved.getNoKelompok().intValue(),
            saved.getTopik(),
            saved.getKapasitas().intValue(),
            request.durasiMenit() != null ? request.durasiMenit() : 30,
            "Menunggu Mahasiswa",
            new ArrayList<>()
        );
    }

    public List<KelompokDetailDTO> buildFallbackKelompokList() {
        return List.of(
            new KelompokDetailDTO(
                1,
                1,
                "Sistem Pendeteksi Intrusi Jaringan",
                4,
                30,
                "Menunggu Mahasiswa",
                List.of(
                    new AnggotaItemDTO("6182101041", "Budi"),
                    new AnggotaItemDTO("6182101044", "Citra"),
                    new AnggotaItemDTO("6182101034", "Andi")
                )
            ),
            new KelompokDetailDTO(
                2,
                2,
                "Perancangan Topologi Jaringan Kampus",
                4,
                30,
                "Lengkap",
                List.of(
                    new AnggotaItemDTO("6182101001", "Andi Saputra"),
                    new AnggotaItemDTO("6182101002", "Nurul Hidayah"),
                    new AnggotaItemDTO("6182101003", "Rizky Maulana"),
                    new AnggotaItemDTO("6182101004", "Salsabila Putri")
                )
            ),
            new KelompokDetailDTO(
                3,
                3,
                "E-Commerce UMKM",
                5,
                30,
                "Menunggu Mahasiswa",
                List.of(
                    new AnggotaItemDTO("6182101015", "Hana"),
                    new AnggotaItemDTO("6182101016", "Iqbal")
                )
            )
        );
    }
}
