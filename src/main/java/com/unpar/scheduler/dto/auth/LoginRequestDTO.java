package com.unpar.scheduler.dto.auth;

public record LoginRequestDTO(
    String email,
    String identifier,
    String password,
    String expectedRole
) {
    public String resolvedIdentifier() {
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        if (identifier != null && !identifier.isBlank()) {
            return identifier.trim();
        }
        return "";
    }
}
