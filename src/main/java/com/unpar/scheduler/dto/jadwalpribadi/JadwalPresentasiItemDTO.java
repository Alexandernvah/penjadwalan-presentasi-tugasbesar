package com.unpar.scheduler.dto.jadwalpribadi;

import java.util.List;

public record JadwalPresentasiItemDTO(
    Integer idPresentasi,
    String tanggal,
    String waktuMulai,
    String waktuSelesai,
    String kodeMatkul,
    String namaMatkul,
    String namaKelompok,
    String topik,
    String ruangan,
    List<MahasiswaSederhanaDTO> anggotaKelompok
) {
}
