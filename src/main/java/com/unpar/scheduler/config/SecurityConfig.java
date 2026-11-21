package com.unpar.scheduler.config;

import com.unpar.scheduler.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthService authService;

    public SecurityConfig(AuthService authService) {
        this.authService = authService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .addFilterBefore(jwtAndNoCacheFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/dosen/index.html",
                    "/admin/index.html",
                    "/mahasiswa/index.html",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/assets/**",
                    "/favicon.ico",
                    "/error",
                    "/api/auth/login",
                    "/api/auth/logout",
                    "/api/auth/me"
                ).permitAll()
                .requestMatchers("/dosen/**", "/api/dosen/**").hasRole("DOSEN")
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/mahasiswa/**", "/api/mahasiswa/**").hasRole("MAHASISWA")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthenticationEntryPoint())
                .accessDeniedHandler(customAccessDeniedHandler())
            )
            .build();
    }

    private OncePerRequestFilter jwtAndNoCacheFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
            ) throws ServletException, IOException {
                String uri = extractPathWithinApp(request);
                if (isProtectedPortalPath(uri)) {
                    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                    response.setHeader("Pragma", "no-cache");
                    response.setDateHeader("Expires", 0);
                }

                Authentication existing = SecurityContextHolder.getContext().getAuthentication();
                if (existing == null || !(existing.getPrincipal() instanceof AuthenticatedUserPrincipal)) {
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring("Bearer ".length()).trim();
                        authService.parseToken(token).ifPresent(principal -> {
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))
                            );
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        });
                    }
                }

                filterChain.doFilter(request, response);
            }
        };
    }

    private AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            String uri = extractPathWithinApp(request);
            if (uri.startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Sesi berakhir atau belum login\"}");
                return;
            }
            if (uri.startsWith("/dosen/") || "/dosen".equals(uri)) {
                response.sendRedirect(request.getContextPath() + "/dosen/index.html");
            } else if (uri.startsWith("/admin/") || "/admin".equals(uri)) {
                response.sendRedirect(request.getContextPath() + "/admin/index.html");
            } else if (uri.startsWith("/mahasiswa/") || "/mahasiswa".equals(uri)) {
                response.sendRedirect(request.getContextPath() + "/mahasiswa/index.html");
            } else {
                response.sendRedirect(request.getContextPath() + "/index.html");
            }
        };
    }

    private AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            String uri = extractPathWithinApp(request);
            if (uri.startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Akses ditolak untuk role Anda\"}");
                return;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String redirectPath = resolveRoleDashboardPath(auth);
            response.sendRedirect(request.getContextPath() + redirectPath);
        };
    }

    private static String resolveRoleDashboardPath(Authentication auth) {
        if (auth != null && auth.getAuthorities() != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                String role = authority.getAuthority();
                if ("ROLE_DOSEN".equals(role)) {
                    return "/dosen/dashboard.html";
                }
                if ("ROLE_ADMIN".equals(role)) {
                    return "/admin/dashboard.html";
                }
                if ("ROLE_MAHASISWA".equals(role)) {
                    return "/mahasiswa/dashboard.html";
                }
            }
        }
        return "/index.html";
    }

    private static boolean isProtectedPortalPath(String uri) {
        return uri.startsWith("/dosen/")
            || uri.startsWith("/admin/")
            || uri.startsWith("/mahasiswa/");
    }

    private static String extractPathWithinApp(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
