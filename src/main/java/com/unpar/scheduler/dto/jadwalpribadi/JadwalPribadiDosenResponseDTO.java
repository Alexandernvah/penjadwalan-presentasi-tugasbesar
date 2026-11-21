package com.unpar.scheduler.dto.jadwalpribadi;

import java.util.List;

public record JadwalPribadiDosenResponseDTO(
    List<JadwalRutinItemDTO> jadwalRutin,
    List<JadwalUjianItemDTO> jadwalUjian,
    List<JadwalPresentasiItemDTO> jadwalPresentasi
) {
}
