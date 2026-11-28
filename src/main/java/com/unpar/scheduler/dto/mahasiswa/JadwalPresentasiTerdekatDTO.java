package com.unpar.scheduler.dto.mahasiswa;

import java.time.LocalDate;
import java.time.LocalTime;

public record JadwalPresentasiTerdekatDTO(
    String kodeMatkul,
    String namaMatkul,
    LocalDate tanggal,
    LocalTime jamMulai,
    LocalTime jamSelesai,
    String ruangan,
    Short noKelompok,
    String status
) {
}
