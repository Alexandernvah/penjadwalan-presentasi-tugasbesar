package com.unpar.scheduler.service;

import com.unpar.scheduler.config.AuthenticatedUserPrincipal;
import com.unpar.scheduler.dto.auth.LoginRequestDTO;
import com.unpar.scheduler.dto.auth.LoginResponseDTO;
import com.unpar.scheduler.model.entity.Akun;
import com.unpar.scheduler.model.entity.Dosen;
import com.unpar.scheduler.model.entity.Mahasiswa;
import com.unpar.scheduler.model.entity.Role;
import com.unpar.scheduler.repository.AkunRepository;
import com.unpar.scheduler.repository.DosenRepository;
import com.unpar.scheduler.repository.MahasiswaRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String JWT_SECRET = "unpar-tubes-scheduler-jwt-secret-key-2026-informatika-secure";
    private static final long JWT_EXPIRATION_MS = 24L * 60L * 60L * 1000L;

    private final AkunRepository akunRepository;
    private final DosenRepository dosenRepository;
    private final MahasiswaRepository mahasiswaRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey signingKey;

    public AuthService(
        AkunRepository akunRepository,
        DosenRepository dosenRepository,
        MahasiswaRepository mahasiswaRepository
    ) {
        this.akunRepository = akunRepository;
        this.dosenRepository = dosenRepository;
        this.mahasiswaRepository = mahasiswaRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.signingKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public LoginResponseDTO authenticate(LoginRequestDTO request) {
        if (request == null) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Permintaan login tidak valid.");
        }

        String identifier = request.resolvedIdentifier();
        String rawPassword = request.password();

        if (identifier.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Email/NIDN/NPM dan password wajib diisi.");
        }

        ResolvedIdentity identity = resolveIdentity(identifier)
            .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Email/NIDN/NPM atau password tidak valid."));

        Akun akun = identity.akun();
        if (!Boolean.TRUE.equals(akun.getAktif())) {
            throw new AuthException(HttpStatus.FORBIDDEN, "Akun Anda sedang dinonaktifkan.");
        }

        if (!verifyPassword(rawPassword, akun.getPassword())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Email/NIDN/NPM atau password tidak valid.");
        }

        String actualRole = akun.getRole().name();
        String expectedRole = normalizeRole(request.expectedRole());
        if (!expectedRole.isEmpty() && !expectedRole.equals(actualRole)) {
            throw new AuthException(HttpStatus.FORBIDDEN, "Akun Anda tidak memiliki hak akses untuk portal ini.");
        }

        String token = generateToken(identity.id(), identity.nidn(), identity.nama(), akun.getEmail(), actualRole);
        return new LoginResponseDTO(
            token,
            actualRole,
            identity.id(),
            identity.nidn(),
            identity.nama(),
            akun.getEmail()
        );
    }

    public Optional<AuthenticatedUserPrincipal> parseToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();

            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            String id = claims.get("id", String.class);
            String nidn = claims.get("nidn", String.class);
            String nama = claims.get("nama", String.class);
            if (role == null || role.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUserPrincipal(id, nidn, nama, email, role));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    boolean verifyPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        String normalizedStored = storedPassword.startsWith("{bcrypt}")
            ? storedPassword.substring("{bcrypt}".length())
            : storedPassword;

        if (normalizedStored.startsWith("$2a$")
            || normalizedStored.startsWith("$2b$")
            || normalizedStored.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, normalizedStored);
        }
        return storedPassword.equals(rawPassword);
    }

    private Optional<ResolvedIdentity> resolveIdentity(String identifier) {
        Optional<Akun> byEmail = akunRepository.findByEmailIgnoreCase(identifier);
        if (byEmail.isPresent()) {
            return Optional.of(buildIdentityFromAkun(byEmail.get(), null, null));
        }

        Optional<Dosen> byNidn = dosenRepository.findByNomorIndukDosen(identifier);
        if (byNidn.isPresent() && byNidn.get().getAkun() != null) {
            Dosen dosen = byNidn.get();
            return Optional.of(buildIdentityFromAkun(dosen.getAkun(), dosen, null));
        }

        Optional<Mahasiswa> byNpm = mahasiswaRepository.findByNpm(identifier);
        if (byNpm.isPresent() && byNpm.get().getAkun() != null) {
            Mahasiswa mhs = byNpm.get();
            return Optional.of(buildIdentityFromAkun(mhs.getAkun(), null, mhs));
        }

        return Optional.empty();
    }

    private ResolvedIdentity buildIdentityFromAkun(Akun akun, Dosen matchedDosen, Mahasiswa matchedMahasiswa) {
        Role role = akun.getRole();
        if (role == Role.DOSEN) {
            Dosen dosen = matchedDosen != null
                ? matchedDosen
                : dosenRepository.findByAkun_IdAkun(akun.getIdAkun())
                    .or(() -> dosenRepository.findByAkun_Email(akun.getEmail()))
                    .orElse(null);
            String nidn = (dosen != null && dosen.getNomorIndukDosen() != null)
                ? dosen.getNomorIndukDosen().trim()
                : String.valueOf(akun.getIdAkun());
            String nama = (dosen != null && dosen.getNama() != null)
                ? dosen.getNama()
                : akun.getEmail();
            return new ResolvedIdentity(akun, nidn, nidn, nama);
        }

        if (role == Role.MAHASISWA) {
            Mahasiswa mhs = matchedMahasiswa != null
                ? matchedMahasiswa
                : mahasiswaRepository.findByAkun_IdAkun(akun.getIdAkun())
                    .or(() -> mahasiswaRepository.findByAkun_EmailIgnoreCase(akun.getEmail()))
                    .orElse(null);
            String npm = (mhs != null && mhs.getNpm() != null)
                ? mhs.getNpm().trim()
                : String.valueOf(akun.getIdAkun());
            String nama = (mhs != null && mhs.getNama() != null)
                ? mhs.getNama()
                : akun.getEmail();
            return new ResolvedIdentity(akun, npm, npm, nama);
        }

        String adminId = String.valueOf(akun.getIdAkun() != null ? akun.getIdAkun() : "ADMIN");
        return new ResolvedIdentity(akun, adminId, adminId, "Administrator");
    }

    private String generateToken(String id, String nidn, String nama, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + JWT_EXPIRATION_MS);
        return Jwts.builder()
            .subject(email)
            .claim("role", role)
            .claim("id", id)
            .claim("nidn", nidn)
            .claim("nama", nama)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact();
    }

    private static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }
        String upper = role.trim().toUpperCase();
        return upper.startsWith("ROLE_") ? upper.substring("ROLE_".length()) : upper;
    }

    private record ResolvedIdentity(
        Akun akun,
        String id,
        String nidn,
        String nama
    ) {
    }
}
