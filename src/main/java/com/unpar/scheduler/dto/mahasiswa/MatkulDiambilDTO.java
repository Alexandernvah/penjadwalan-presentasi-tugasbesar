package com.unpar.scheduler.dto.mahasiswa;

public record MatkulDiambilDTO(
    Integer idKelas,
    String kodeMatkul,
    String namaMatkul,
    String kodeKelas,
    String namaDosen
) {
}
