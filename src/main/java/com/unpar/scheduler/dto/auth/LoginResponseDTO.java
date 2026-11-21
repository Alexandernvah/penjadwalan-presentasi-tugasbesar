package com.unpar.scheduler.dto.auth;

public record LoginResponseDTO(
    String token,
    String role,
    String id,
    String nidn,
    String nama,
    String email
) {
}
