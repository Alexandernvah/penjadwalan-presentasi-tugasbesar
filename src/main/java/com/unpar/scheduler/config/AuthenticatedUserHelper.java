package com.unpar.scheduler.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserHelper {

    public static String resolveDosenNidn(String requestedDosenId) {
        if (requestedDosenId != null && !requestedDosenId.isBlank()) {
            return requestedDosenId.trim();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            if (principal.nidn() != null && !principal.nidn().isBlank()) {
                return principal.nidn().trim();
            }
            if (principal.id() != null && !principal.id().isBlank()) {
                return principal.id().trim();
            }
        }
        return requestedDosenId;
    }

    public static AuthenticatedUserPrincipal getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            return principal;
        }
        return null;
    }
}
