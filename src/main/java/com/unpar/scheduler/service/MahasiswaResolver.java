package com.unpar.scheduler.service;

import com.unpar.scheduler.config.AuthenticatedUserPrincipal;
import com.unpar.scheduler.model.entity.Akun;
import com.unpar.scheduler.model.entity.Mahasiswa;
import com.unpar.scheduler.model.entity.Role;
import com.unpar.scheduler.repository.AkunRepository;
import com.unpar.scheduler.repository.MahasiswaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

// Menerjemahkan Authentication Spring Security (email -> Akun -> Mahasiswa).
@Component
public class MahasiswaResolver {

    private static final String ROLE_MAHASISWA = "MAHASISWA";
    private static final String AUTHORITY_MAHASISWA = "ROLE_MAHASISWA";
    private static final String ANONYMOUS_USER = "anonymousUser";

    private final AkunRepository akunRepository;
    private final MahasiswaRepository mahasiswaRepository;

    public MahasiswaResolver(AkunRepository akunRepository, MahasiswaRepository mahasiswaRepository) {
        this.akunRepository = akunRepository;
        this.mahasiswaRepository = mahasiswaRepository;
    }

    public Mahasiswa resolve(Authentication authentication) {
        String email = extractEmail(authentication);
        Akun akun = akunRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new MahasiswaException(HttpStatus.NOT_FOUND, "Akun mahasiswa tidak ditemukan."));
        if (!Boolean.TRUE.equals(akun.getAktif())) {
            throw new MahasiswaException(HttpStatus.FORBIDDEN, "Akun mahasiswa tidak aktif.");
        }
        if (akun.getRole() != Role.MAHASISWA) {
            throw new MahasiswaException(HttpStatus.FORBIDDEN, "Akses ditolak untuk role Anda.");
        }
        return mahasiswaRepository.findByAkun_IdAkun(akun.getIdAkun())
            .orElseThrow(() -> new MahasiswaException(HttpStatus.NOT_FOUND, "Data mahasiswa tidak ditemukan."));
    }

    private String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || ANONYMOUS_USER.equals(authentication.getPrincipal())) {
            throw new MahasiswaException(HttpStatus.UNAUTHORIZED, "Sesi berakhir atau belum login.");
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            if (!ROLE_MAHASISWA.equals(principal.role())) {
                throw new MahasiswaException(HttpStatus.FORBIDDEN, "Akses ditolak untuk role Anda.");
            }
            if (principal.email() == null || principal.email().isBlank()) {
                throw new MahasiswaException(HttpStatus.UNAUTHORIZED, "Sesi berakhir atau belum login.");
            }
            return principal.email().trim();
        }
        boolean hasMahasiswaAuthority = authentication.getAuthorities() != null
            && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(AUTHORITY_MAHASISWA::equals);
        if (!hasMahasiswaAuthority) {
            throw new MahasiswaException(HttpStatus.FORBIDDEN, "Akses ditolak untuk role Anda.");
        }
        String name = authentication.getName();
        if (name == null || name.isBlank()) {
            throw new MahasiswaException(HttpStatus.UNAUTHORIZED, "Sesi berakhir atau belum login.");
        }
        return name.trim();
    }
}
