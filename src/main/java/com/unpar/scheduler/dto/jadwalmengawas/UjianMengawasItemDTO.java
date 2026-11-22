package com.unpar.scheduler.dto.jadwalmengawas;

import java.util.List;

public record UjianMengawasItemDTO(
    Integer idUjian,
    String kodeMatkul,
    String namaMatkul,
    String namaKelas,
    String jenisUjian,
    String tanggal,
    String hari,
    String jamMulai,
    String jamSelesai,
    String ruangan,
    List<DosenRingkasDTO> pengawasList,
    int jumlahPengawas,
    boolean isAssignedToMe,
    boolean hasConflict,
    List<String> conflictDetails
) {
}
