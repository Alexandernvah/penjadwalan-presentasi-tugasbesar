package com.unpar.scheduler.controller;

import com.unpar.scheduler.config.AuthenticatedUserHelper;
import com.unpar.scheduler.config.AuthenticatedUserPrincipal;
import com.unpar.scheduler.dto.auth.LoginRequestDTO;
import com.unpar.scheduler.dto.auth.LoginResponseDTO;
import com.unpar.scheduler.service.AuthException;
import com.unpar.scheduler.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
        @RequestBody LoginRequestDTO request,
        HttpServletRequest httpRequest
    ) {
        LoginResponseDTO result = authService.authenticate(request);

        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
            result.id(),
            result.nidn(),
            result.nama(),
            result.email(),
            result.role()
        );
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + result.role()))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        session.setAttribute("userId", result.id());
        session.setAttribute("userName", result.nama());
        session.setAttribute("userRole", result.role());
        session.setAttribute("userEmail", result.email());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal principal = AuthenticatedUserHelper.getCurrentPrincipal();
        if (principal == null) {
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                Object roleObj = session.getAttribute("userRole");
                if (roleObj instanceof String role && !role.isBlank()) {
                    String id = (String) session.getAttribute("userId");
                    String nama = (String) session.getAttribute("userName");
                    String email = (String) session.getAttribute("userEmail");
                    principal = new AuthenticatedUserPrincipal(id, id, nama, email, role);
                }
            }
        }

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("authenticated", false));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("authenticated", true);
        body.put("role", principal.role());
        body.put("id", principal.id());
        body.put("nidn", principal.nidn());
        body.put("nama", principal.nama());
        body.put("email", principal.email());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("message", "Logout berhasil"));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(AuthException ex) {
        return ResponseEntity.status(ex.getStatus())
            .body(Map.of("message", ex.getMessage()));
    }
}
