package com.unpar.scheduler.dto.penjadwalan;

import java.util.List;

public record SlotMatriksDTO(
    String jamMulai,
    String jamSelesai,
    String labelWaktu,
    String status,
    String alasanUtama,
    List<KonflikDetailDTO> konflikList,
    PresentasiTerjadwalDTO presentasi
) {
}
