package com.unpar.scheduler.dto.mahasiswa;

import java.util.List;

public record MatkulMahasiswaResponseDTO(
    String namaMahasiswa,
    String npm,
    String semesterAkademikLabel,
    boolean sudahTerkunci,
    List<MataKuliahTersediaDTO> daftarMataKuliah
) {
}
