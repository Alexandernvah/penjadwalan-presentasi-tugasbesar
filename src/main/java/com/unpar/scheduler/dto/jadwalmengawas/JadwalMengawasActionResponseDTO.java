package com.unpar.scheduler.dto.jadwalmengawas;

import java.util.List;

public record JadwalMengawasActionResponseDTO(
    boolean success,
    boolean conflict,
    String message,
    List<String> conflicts
) {
}
