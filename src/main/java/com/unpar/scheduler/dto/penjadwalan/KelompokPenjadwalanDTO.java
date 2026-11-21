package com.unpar.scheduler.dto.penjadwalan;

import java.util.List;

public record KelompokPenjadwalanDTO(
    Integer idKelompok,
    Integer noKelompok,
    Integer nomorKelompok,
    String topik,
    Integer batasKapasitas,
    Integer durasiMenit,
    Boolean terjadwal,
    Integer idPresentasi,
    String hariNama,
    String tanggal,
    String jamMulai,
    String jamSelesai,
    String ruangan,
    String catatan,
    List<AnggotaPenjadwalanDTO> anggota
) {
}
