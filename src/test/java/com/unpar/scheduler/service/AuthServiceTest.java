package com.unpar.scheduler.service;

import com.unpar.scheduler.config.AuthenticatedUserHelper;
import com.unpar.scheduler.config.AuthenticatedUserPrincipal;
import com.unpar.scheduler.controller.AuthController;
import com.unpar.scheduler.dto.auth.LoginRequestDTO;
import com.unpar.scheduler.dto.auth.LoginResponseDTO;
import com.unpar.scheduler.model.entity.Akun;
import com.unpar.scheduler.model.entity.Dosen;
import com.unpar.scheduler.model.entity.Mahasiswa;
import com.unpar.scheduler.model.entity.Role;
import com.unpar.scheduler.repository.AkunRepository;
import com.unpar.scheduler.repository.DosenRepository;
import com.unpar.scheduler.repository.MahasiswaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AkunRepository akunRepository;
    @Mock
    private DosenRepository dosenRepository;
    @Mock
    private MahasiswaRepository mahasiswaRepository;

    private AuthService authService;
    private AuthController authController;

    private Akun akunDosen;
    private Dosen dosenBudi;
    private Akun akunMahasiswa;
    private Mahasiswa mhsAndi;

    @BeforeEach
    void setUp() {
        authService = new AuthService(akunRepository, dosenRepository, mahasiswaRepository);
        authController = new AuthController(authService);
        SecurityContextHolder.clearContext();

        String bcryptHash = new BCryptPasswordEncoder().encode("secret123");
        akunDosen = new Akun(1, "budi.santoso@unpar.ac.id", bcryptHash, Role.DOSEN, true);
        dosenBudi = new Dosen("1975031501", "Dr. Budi Santoso", akunDosen);

        akunMahasiswa = new Akun(2, "andi@student.unpar.ac.id", "mhsPass", Role.MAHASISWA, true);
        mhsAndi = new Mahasiswa("6182101001", "Andi Saputra", akunMahasiswa);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginWithBcryptPasswordAndMatchingRoleSucceedsAndProducesValidJwt() {
        when(akunRepository.findByEmailIgnoreCase("budi.santoso@unpar.ac.id")).thenReturn(Optional.of(akunDosen));
        when(dosenRepository.findByAkun_IdAkun(1)).thenReturn(Optional.of(dosenBudi));

        LoginResponseDTO response = authService.authenticate(
            new LoginRequestDTO("budi.santoso@unpar.ac.id", null, "secret123", "DOSEN")
        );

        assertNotNull(response.token());
        assertEquals("DOSEN", response.role());
        assertEquals("1975031501", response.id());
        assertEquals("1975031501", response.nidn());
        assertEquals("Dr. Budi Santoso", response.nama());
        assertEquals("budi.santoso@unpar.ac.id", response.email());

        Optional<AuthenticatedUserPrincipal> parsed = authService.parseToken(response.token());
        assertTrue(parsed.isPresent());
        assertEquals("1975031501", parsed.get().nidn());
        assertEquals("DOSEN", parsed.get().role());
    }

    @Test
    void loginWithNidnOrNpmAndPlaintextSeedPasswordSucceeds() {
        when(akunRepository.findByEmailIgnoreCase("6182101001")).thenReturn(Optional.empty());
        when(dosenRepository.findByNomorIndukDosen("6182101001")).thenReturn(Optional.empty());
        when(mahasiswaRepository.findByNpm("6182101001")).thenReturn(Optional.of(mhsAndi));

        LoginResponseDTO response = authService.authenticate(
            new LoginRequestDTO("6182101001", null, "mhsPass", "MAHASISWA")
        );

        assertEquals("MAHASISWA", response.role());
        assertEquals("6182101001", response.id());
        assertEquals("Andi Saputra", response.nama());
    }

    @Test
    void loginWithMismatchedPortalRoleReturns403Forbidden() {
        when(akunRepository.findByEmailIgnoreCase("budi.santoso@unpar.ac.id")).thenReturn(Optional.of(akunDosen));
        when(dosenRepository.findByAkun_IdAkun(1)).thenReturn(Optional.of(dosenBudi));

        AuthException ex = assertThrows(AuthException.class, () ->
            authService.authenticate(new LoginRequestDTO("budi.santoso@unpar.ac.id", null, "secret123", "ADMIN"))
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Akun Anda tidak memiliki hak akses untuk portal ini.", ex.getMessage());
    }

    @Test
    void loginWithWrongPasswordOrInactiveAccountIsRejected() {
        when(akunRepository.findByEmailIgnoreCase("budi.santoso@unpar.ac.id")).thenReturn(Optional.of(akunDosen));
        when(dosenRepository.findByAkun_IdAkun(1)).thenReturn(Optional.of(dosenBudi));

        AuthException wrongPass = assertThrows(AuthException.class, () ->
            authService.authenticate(new LoginRequestDTO("budi.santoso@unpar.ac.id", null, "wrongPass", "DOSEN"))
        );
        assertEquals(HttpStatus.UNAUTHORIZED, wrongPass.getStatus());

        akunDosen.setAktif(false);
        AuthException inactive = assertThrows(AuthException.class, () ->
            authService.authenticate(new LoginRequestDTO("budi.santoso@unpar.ac.id", null, "secret123", "DOSEN"))
        );
        assertEquals(HttpStatus.FORBIDDEN, inactive.getStatus());
    }

    @Test
    void authControllerEstablishesSessionResolvesNidnAndInvalidatesOnLogout() {
        when(akunRepository.findByEmailIgnoreCase("budi.santoso@unpar.ac.id")).thenReturn(Optional.of(akunDosen));
        when(dosenRepository.findByAkun_IdAkun(1)).thenReturn(Optional.of(dosenBudi));

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        ResponseEntity<LoginResponseDTO> loginRes = authController.login(
            new LoginRequestDTO("budi.santoso@unpar.ac.id", null, "secret123", "DOSEN"),
            req
        );
        assertEquals(HttpStatus.OK, loginRes.getStatusCode());
        assertEquals("1975031501", AuthenticatedUserHelper.resolveDosenNidn(null));

        ResponseEntity<Map<String, Object>> meRes = authController.me(req);
        assertEquals(HttpStatus.OK, meRes.getStatusCode());
        assertEquals(Boolean.TRUE, meRes.getBody().get("authenticated"));
        assertEquals("DOSEN", meRes.getBody().get("role"));

        ResponseEntity<Map<String, String>> logoutRes = authController.logout(req, res);
        assertEquals(HttpStatus.OK, logoutRes.getStatusCode());
        assertNull(AuthenticatedUserHelper.getCurrentPrincipal());

        ResponseEntity<Map<String, Object>> meAfterLogout = authController.me(req);
        assertEquals(HttpStatus.UNAUTHORIZED, meAfterLogout.getStatusCode());
        assertEquals(Boolean.FALSE, meAfterLogout.getBody().get("authenticated"));
        assertNotNull(res.getCookie("JSESSIONID"));
        assertEquals(0, res.getCookie("JSESSIONID").getMaxAge());
        assertFalse(SecurityContextHolder.getContext().getAuthentication() != null);
    }
}
