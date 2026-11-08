package com.unpar.scheduler.dto.dashboard;

import java.time.LocalTime;

public record MataKuliahAjarItemDTO(
    Integer idKelas,
    String idMatkul,
    String namaMatkul,
    String kodeKelas,
    Short sks,
    String ruangan,
    Short hari,
    LocalTime jamMulai,
    LocalTime jamSelesai
) {
}
