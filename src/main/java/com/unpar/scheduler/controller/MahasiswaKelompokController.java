package com.unpar.scheduler.controller;

import com.unpar.scheduler.dto.mahasiswa.DaftarKelompokKelasResponseDTO;
import com.unpar.scheduler.dto.mahasiswa.KelompokMataKuliahResponseDTO;
import com.unpar.scheduler.service.MahasiswaKelompokService;
import com.unpar.scheduler.service.MahasiswaResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mahasiswa/kelompok")
public class MahasiswaKelompokController {

    private final MahasiswaKelompokService mahasiswaKelompokService;
    private final MahasiswaResolver mahasiswaResolver;

    public MahasiswaKelompokController(
        MahasiswaKelompokService mahasiswaKelompokService,
        MahasiswaResolver mahasiswaResolver
    ) {
        this.mahasiswaKelompokService = mahasiswaKelompokService;
        this.mahasiswaResolver = mahasiswaResolver;
    }

    @GetMapping("/mata-kuliah")
    public ResponseEntity<KelompokMataKuliahResponseDTO> getMataKuliahDiambil(Authentication authentication) {
        var mahasiswa = mahasiswaResolver.resolve(authentication);
        return ResponseEntity.ok(mahasiswaKelompokService.getMataKuliahDiambil(mahasiswa));
    }

    @GetMapping
    public ResponseEntity<DaftarKelompokKelasResponseDTO> getDaftarKelompok(
        Authentication authentication,
        @RequestParam(name = "idKelas", required = false) Integer idKelas
    ) {
        var mahasiswa = mahasiswaResolver.resolve(authentication);
        return ResponseEntity.ok(mahasiswaKelompokService.getDaftarKelompok(mahasiswa, idKelas));
    }

    @PostMapping("/{idKelompok}/gabung")
    public ResponseEntity<DaftarKelompokKelasResponseDTO> gabungKelompok(
        Authentication authentication,
        @PathVariable("idKelompok") Integer idKelompok
    ) {
        var mahasiswa = mahasiswaResolver.resolve(authentication);
        return ResponseEntity.ok(mahasiswaKelompokService.gabungKelompok(mahasiswa, idKelompok));
    }

    @DeleteMapping("/{idKelompok}/keluar")
    public ResponseEntity<DaftarKelompokKelasResponseDTO> keluarKelompok(
        Authentication authentication,
        @PathVariable("idKelompok") Integer idKelompok
    ) {
        var mahasiswa = mahasiswaResolver.resolve(authentication);
        return ResponseEntity.ok(mahasiswaKelompokService.keluarKelompok(mahasiswa, idKelompok));
    }
}
