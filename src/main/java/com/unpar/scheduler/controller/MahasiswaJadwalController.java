package com.unpar.scheduler.controller;

import com.unpar.scheduler.dto.mahasiswa.MahasiswaJadwalResponseDTO;
import com.unpar.scheduler.service.MahasiswaJadwalService;
import com.unpar.scheduler.service.MahasiswaResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/mahasiswa/jadwal")
public class MahasiswaJadwalController {

    private final MahasiswaJadwalService mahasiswaJadwalService;
    private final MahasiswaResolver mahasiswaResolver;

    public MahasiswaJadwalController(
        MahasiswaJadwalService mahasiswaJadwalService,
        MahasiswaResolver mahasiswaResolver
    ) {
        this.mahasiswaJadwalService = mahasiswaJadwalService;
        this.mahasiswaResolver = mahasiswaResolver;
    }

    @GetMapping
    public ResponseEntity<MahasiswaJadwalResponseDTO> getJadwal(Authentication authentication) {
        var mahasiswa = mahasiswaResolver.resolve(authentication);
        return ResponseEntity.ok(mahasiswaJadwalService.getJadwal(mahasiswa, LocalDate.now()));
    }
}
