package com.unpar.scheduler.dto.mahasiswa;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AnggotaKelompokItemDTO(
    String npm,
    String nama,
    @JsonProperty("isCurrentUser") boolean isCurrentUser
) {
}
