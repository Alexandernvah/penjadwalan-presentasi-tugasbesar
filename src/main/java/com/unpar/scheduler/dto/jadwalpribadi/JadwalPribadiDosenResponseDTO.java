package com.unpar.scheduler.dto.jadwalpribadi;

import java.util.List;

public record JadwalPribadiDosenResponseDTO(
    String semesterAktif,
    List<JadwalRutinItemDTO> jadwalRutin,
    List<JadwalUjianItemDTO> jadwalUjian,
    List<JadwalPresentasiItemDTO> jadwalPresentasi
) {
}
