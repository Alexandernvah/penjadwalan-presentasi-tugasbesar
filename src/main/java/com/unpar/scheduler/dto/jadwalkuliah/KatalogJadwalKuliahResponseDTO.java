package com.unpar.scheduler.dto.jadwalkuliah;

import java.util.List;

public record KatalogJadwalKuliahResponseDTO(
    String semesterAkademik,
    List<MataKuliahPilihanDTO> mataKuliah
) {
}
