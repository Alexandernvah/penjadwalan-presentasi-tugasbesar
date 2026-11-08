package com.unpar.scheduler.dto.kelompok;

public record BatchCreateKelompokRequestDTO(
    Integer idKelas,
    Integer jumlahKelompok,
    Integer batasKapasitas,
    Integer durasiMenit,
    String defaultTopik
) {}
