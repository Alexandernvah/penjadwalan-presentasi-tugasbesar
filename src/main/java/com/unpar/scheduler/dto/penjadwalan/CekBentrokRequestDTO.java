package com.unpar.scheduler.dto.penjadwalan;

public record CekBentrokRequestDTO(
    Integer idKelas,
    Integer idKelompok,
    String ruangan,
    String tanggal,
    String jamMulai,
    Integer durasiMenit,
    String dosenId
) {
}
