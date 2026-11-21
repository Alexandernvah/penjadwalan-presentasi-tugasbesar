package com.unpar.scheduler.dto.penjadwalan;

public record SelMatriksMingguanDTO(
    int hariKe,
    String namaHari,
    String tanggal,
    String labelHariTanggal,
    String jamMulai,
    String jamSelesai,
    String labelRentangJam,
    String status,
    String badgeText,
    String deskripsi,
    String daftarMahasiswaBentrok,
    String namaAgendaBentrok,
    int rowSpan,
    boolean skipRender,
    Integer idPresentasiTerjadwal
) {
}
