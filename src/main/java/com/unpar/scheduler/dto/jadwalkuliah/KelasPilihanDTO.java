package com.unpar.scheduler.dto.jadwalkuliah;

import java.util.List;

public record KelasPilihanDTO(
    Integer idKelas,
    String kodeKelas,
    String dosenPenanggungJawab,
    List<SlotKuliahDTO> jadwal,
    boolean terdaftar
) {
}
