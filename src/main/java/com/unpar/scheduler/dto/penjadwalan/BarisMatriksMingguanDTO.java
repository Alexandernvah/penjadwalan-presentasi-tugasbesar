package com.unpar.scheduler.dto.penjadwalan;

import java.util.List;

public record BarisMatriksMingguanDTO(
    String jamMulai,
    String jamSelesai,
    String labelWaktu,
    List<SelMatriksMingguanDTO> selList
) {
}
