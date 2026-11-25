package com.unpar.scheduler.dto.jadwalkuliah;

import java.util.List;

public record MataKuliahPilihanDTO(
    Integer idMatkulBuka,
    String idMatkul,
    String nama,
    Short sks,
    Integer idKelasTerdaftar,
    List<KelasPilihanDTO> kelas
) {
}
