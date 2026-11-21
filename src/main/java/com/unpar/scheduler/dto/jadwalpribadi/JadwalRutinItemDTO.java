package com.unpar.scheduler.dto.jadwalpribadi;

import java.util.List;

public record JadwalRutinItemDTO(
    Integer idKelas,
    String kodeMatkul,
    String namaMatkul,
    String namaKelas,
    String jadwalRingkas,
    String ruangan,
    Integer jumlahMahasiswa,
    List<MahasiswaSederhanaDTO> daftarMahasiswa
) {
}
