package com.unpar.scheduler.dto.jadwalpribadi;

public record JadwalUjianItemDTO(
    Integer idUjian,
    String jenisUjian,
    String tanggal,
    String waktuMulai,
    String waktuSelesai,
    String kodeMatkul,
    String namaMatkul,
    String ruangan
) {
}
