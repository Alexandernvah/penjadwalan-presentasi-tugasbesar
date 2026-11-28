package com.unpar.scheduler.controller;

import com.unpar.scheduler.config.AuthenticatedUserHelper;
import com.unpar.scheduler.config.AuthenticatedUserPrincipal;
import com.unpar.scheduler.dto.matkul.MatkulTersediaResponseDTO;
import com.unpar.scheduler.service.MahasiswaMatkulService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Menerima permintaan GET /api/mahasiswa/matkul, mengambil NPM dari akun yang login, meneruskannya ke service, lalu membalas dengan data JSON.

@RestController
@RequestMapping("/api/mahasiswa/matkul")

public class MahasiswaMatkulController {

    private final MahasiswaMatkulService mahasiswaMatkulService;

    public MahasiswaMatkulController(MahasiswaMatkulService mahasiswaMatkulService) {
        this.mahasiswaMatkulService = mahasiswaMatkulService;
    }

    @GetMapping
    public ResponseEntity<MatkulTersediaResponseDTO> getMatkulTersedia() {
        AuthenticatedUserPrincipal principal = AuthenticatedUserHelper.getCurrentPrincipal();
        if (principal == null || principal.id() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(mahasiswaMatkulService.getMatkulTersedia(principal.id().trim()));
    }
}