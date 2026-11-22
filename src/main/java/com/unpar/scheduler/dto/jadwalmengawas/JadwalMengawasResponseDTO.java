package com.unpar.scheduler.dto.jadwalmengawas;

import java.util.List;

public record JadwalMengawasResponseDTO(
    String semesterLabel,
    String semesterCode,
    List<UjianMengawasItemDTO> myProctoringList,
    List<UjianMengawasItemDTO> semesterExams,
    List<DosenRingkasDTO> availableLecturers
) {
}
