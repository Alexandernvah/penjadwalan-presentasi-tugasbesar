package com.unpar.scheduler.dto.jadwalkuliah;

import java.util.List;

public record JadwalKuliahSayaItemDTO(
    Integer idKelas,
    String idMatkul,
    String namaMatkul,
    Short sks,
    String kodeKelas,
    String dosenPenanggungJawab,
    List<SlotKuliahDTO> jadwal
) {
}
