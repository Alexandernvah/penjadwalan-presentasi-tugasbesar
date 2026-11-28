package com.unpar.scheduler.controller;

import com.unpar.scheduler.dto.mahasiswa.MatkulMahasiswaResponseDTO;
import com.unpar.scheduler.dto.mahasiswa.SimpanMatkulRequestDTO;
import com.unpar.scheduler.service.MahasiswaMatkulService;
import com.unpar.scheduler.service.MahasiswaResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mahasiswa/matakuliah")
public class MahasiswaMatkulController {

    private final MahasiswaMatkulService mahasiswaMatkulService;
    private final MahasiswaResolver mahasiswaResolver;

    public MahasiswaMatkulController(
        MahasiswaMatkulService mahasiswaMatkulService,
        MahasiswaResolver mahasiswaResolver
    ) {
        this.mahasiswaMatkulService = mahasiswaMatkulService;
        this.mahasiswaResolver = mahasiswaResolver;
    }

    @GetMapping
    public ResponseEntity<MatkulMahasiswaResponseDTO> getMatkulTersedia(Authentication authentication) {
        var mahasiswa = mahasiswaResolver.resolve(authentication);
        return ResponseEntity.ok(mahasiswaMatkulService.getMatkulTersedia(mahasiswa));
    }

    @PostMapping
    public ResponseEntity<MatkulMahasiswaResponseDTO> simpanMatkul(
        Authentication authentication,
        @RequestBody(required = false) SimpanMatkulRequestDTO request
    ) {
        var mahasiswa = mahasiswaResolver.resolve(authentication);
        var idKelas = request != null ? request.idKelas() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mahasiswaMatkulService.simpanMatkul(mahasiswa, idKelas));
    }
}
