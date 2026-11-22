package com.unpar.scheduler.dto.jadwalmengawas;

public record PindahMengawasRequestDTO(
    Integer sourceUjianId,
    Integer targetUjianId,
    String mode,
    String targetDosenId,
    String replacementDosenId,
    Boolean override
) {
}
