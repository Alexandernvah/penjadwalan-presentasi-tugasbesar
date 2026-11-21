package com.unpar.scheduler.dto.penjadwalan;

import java.util.List;

public record MatriksPenjadwalanResponseDTO(
    Integer idKelas,
    String namaKelas,
    String ruangan,
    String tanggal,
    String hariNama,
    List<SlotMatriksDTO> slots
) {
}
