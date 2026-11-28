package com.unpar.scheduler.dto.mahasiswa;

import java.util.List;

public record MataKuliahTersediaDTO(
    Integer idMatkulBuka,
    String kodeMatkul,
    String namaMatkul,
    List<KelasTersediaDTO> daftarKelas
) {
}
