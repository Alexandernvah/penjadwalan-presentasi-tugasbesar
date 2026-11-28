package com.unpar.scheduler.controller;

import com.unpar.scheduler.dto.mahasiswa.MahasiswaDashboardDTO;
import com.unpar.scheduler.service.MahasiswaDashboardService;
import com.unpar.scheduler.service.MahasiswaResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/mahasiswa/dashboard")
public class MahasiswaDashboardController {

    private final MahasiswaDashboardService mahasiswaDashboardService;
    private final MahasiswaResolver mahasiswaResolver;

    public MahasiswaDashboardController(
        MahasiswaDashboardService mahasiswaDashboardService,
        MahasiswaResolver mahasiswaResolver
    ) {
        this.mahasiswaDashboardService = mahasiswaDashboardService;
        this.mahasiswaResolver = mahasiswaResolver;
    }

    @GetMapping
    public ResponseEntity<MahasiswaDashboardDTO> getDashboard(Authentication authentication) {
        var mahasiswa = mahasiswaResolver.resolve(authentication);
        return ResponseEntity.ok(mahasiswaDashboardService.getDashboard(mahasiswa, LocalDateTime.now()));
    }
}
