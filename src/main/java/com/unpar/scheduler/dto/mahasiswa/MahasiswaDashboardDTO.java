package com.unpar.scheduler.dto.mahasiswa;

import java.util.List;

public record MahasiswaDashboardDTO(
    String namaMahasiswa,
    String npm,
    String semesterAkademikLabel,
    boolean sudahInputMatkul,
    List<MatkulDiambilDTO> daftarMatkulDiambil,
    JadwalPresentasiTerdekatDTO jadwalPresentasiTerdekat
) {
}
