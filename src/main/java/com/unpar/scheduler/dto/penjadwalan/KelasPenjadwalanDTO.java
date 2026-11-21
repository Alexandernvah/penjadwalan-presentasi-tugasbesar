package com.unpar.scheduler.dto.penjadwalan;

public record KelasPenjadwalanDTO(
    Integer idKelas,
    String idMatkul,
    String namaMatkul,
    String kodeKelas,
    Short sks,
    String ruanganDefault,
    String namaDosen,
    String emailDosen
) {
}
