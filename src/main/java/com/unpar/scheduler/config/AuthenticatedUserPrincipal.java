package com.unpar.scheduler.config;

import java.io.Serializable;

public record AuthenticatedUserPrincipal(
    String id,
    String nidn,
    String nama,
    String email,
    String role
) implements Serializable {
}
