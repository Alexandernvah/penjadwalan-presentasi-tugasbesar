package com.unpar.scheduler.dto.mahasiswa;

public record KelasBelumTerjadwalDTO(
    Integer idKelas,
    String kodeMatkul,
    String namaMatkul,
    String kodeKelas,
    boolean sudahPunyaKelompok,
    Short noKelompok
) {
}
