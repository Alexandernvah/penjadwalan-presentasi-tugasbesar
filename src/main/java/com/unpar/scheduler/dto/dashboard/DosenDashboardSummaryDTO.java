package com.unpar.scheduler.dto.dashboard;

import java.util.List;

public record DosenDashboardSummaryDTO(
    String nomorIndukDosen,
    String namaDosen,
    String email,
    String semesterAktif,
    List<MataKuliahAjarItemDTO> mataKuliahAjar
) {
}
