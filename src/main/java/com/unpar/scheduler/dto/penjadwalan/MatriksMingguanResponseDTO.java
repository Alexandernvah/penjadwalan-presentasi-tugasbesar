package com.unpar.scheduler.dto.penjadwalan;

import java.util.List;

public record MatriksMingguanResponseDTO(
    Integer idKelas,
    String namaKelas,
    String idMatkul,
    String namaSingkatMatkul,
    String ruanganDefault,
    Integer idKelompok,
    Integer noKelompok,
    Integer durasiMenit,
    String mode,
    String tanggalMulai,
    String tanggalAkhir,
    String rentangTanggalLabel,
    String prevTanggalMulai,
    String nextTanggalMulai,
    List<HariMingguanDTO> hariList,
    List<BarisMatriksMingguanDTO> barisList
) {
}
