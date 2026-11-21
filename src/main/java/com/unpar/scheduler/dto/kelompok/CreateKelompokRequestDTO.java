package com.unpar.scheduler.dto.kelompok;

public record CreateKelompokRequestDTO(
    Integer idKelas,
    Integer nomorKelompok,
    String topik,
    Integer batasKapasitas,
    Integer durasiMenit
) {
}
