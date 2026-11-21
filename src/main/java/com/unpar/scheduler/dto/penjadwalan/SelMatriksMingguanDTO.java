package com.unpar.scheduler.dto.penjadwalan;

import java.util.Collections;
import java.util.List;

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
    Integer idPresentasiTerjadwal,
    List<String> intervalTerpakai
) {
    public SelMatriksMingguanDTO(
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
        this(
            hariKe,
            namaHari,
            tanggal,
            labelHariTanggal,
            jamMulai,
            jamSelesai,
            labelRentangJam,
            status,
            badgeText,
            deskripsi,
            daftarMahasiswaBentrok,
            namaAgendaBentrok,
            rowSpan,
            skipRender,
            idPresentasiTerjadwal,
            Collections.emptyList()
        );
    }
}

