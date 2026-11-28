package com.unpar.scheduler.dto.mahasiswa;

import java.util.List;

public record MahasiswaJadwalResponseDTO(
    String namaMahasiswa,
    String npm,
    String semesterAkademikLabel,
    List<JadwalPresentasiMahasiswaDTO> daftarJadwal,
    List<KelasBelumTerjadwalDTO> daftarKelasBelumTerjadwal
) {
}
