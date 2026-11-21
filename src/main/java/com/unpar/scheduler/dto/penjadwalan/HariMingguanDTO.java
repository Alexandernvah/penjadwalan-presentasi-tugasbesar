package com.unpar.scheduler.dto.penjadwalan;

public record HariMingguanDTO(
    int hariKe,
    String namaHari,
    String tanggal,
    String labelSingkat,
    String labelLengkap
) {
}
