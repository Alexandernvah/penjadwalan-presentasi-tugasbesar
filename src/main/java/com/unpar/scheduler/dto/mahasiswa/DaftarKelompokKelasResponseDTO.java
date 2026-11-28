package com.unpar.scheduler.dto.mahasiswa;

import java.util.List;

public record DaftarKelompokKelasResponseDTO(
    Integer idKelas,
    String kodeMatkul,
    String namaMatkul,
    String kodeKelas,
    Integer idKelompokTerdaftar,
    Short noKelompokTerdaftar,
    List<KelompokTersediaDTO> daftarKelompok
) {
}
