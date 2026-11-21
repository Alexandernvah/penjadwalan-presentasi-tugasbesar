package com.unpar.scheduler.dto.kelompok;

public record UpdateKelompokRequestDTO(
    String topik,
    Integer batasKapasitas,
    Integer durasiMenit
) {}
