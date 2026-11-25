package com.unpar.scheduler.dto.jadwalkuliah;

public record SlotKuliahDTO(
    Integer hari,
    String namaHari,
    String jamMulai,
    String jamSelesai,
    String ruangan,
    String dosenPengajar,
    String tipePertemuan
) {
}
