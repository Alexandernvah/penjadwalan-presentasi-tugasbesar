package com.unpar.scheduler.dto.penjadwalan;

public record SimpanPresentasiRequestDTO(
    Integer idKelas,
    Integer idKelompok,
    String ruangan,
    String idRuangan,
    String tanggal,
    String jamMulai,
    String jamSelesai,
    Integer durasiMenit,
    String catatan,
    Boolean forceOverride,
    String dosenId
) {
}
