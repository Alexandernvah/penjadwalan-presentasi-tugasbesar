package com.unpar.scheduler.dto.kelompok;

import java.util.List;

public record KelompokDetailDTO(
    Integer idKelompok,
    Integer nomorKelompok,
    String topik,
    Integer batasKapasitas,
    Integer durasiMenit,
    String status,
    List<AnggotaItemDTO> anggota
) {
}
