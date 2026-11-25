package com.unpar.scheduler.controller;

import com.unpar.scheduler.config.AuthenticatedUserHelper;
import com.unpar.scheduler.dto.jadwalkuliah.JadwalKuliahActionResponseDTO;
import com.unpar.scheduler.dto.jadwalkuliah.JadwalKuliahSayaItemDTO;
import com.unpar.scheduler.dto.jadwalkuliah.KatalogJadwalKuliahResponseDTO;
import com.unpar.scheduler.dto.jadwalkuliah.PilihKelasRequestDTO;
import com.unpar.scheduler.service.JadwalKuliahMahasiswaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mahasiswa/jadwal-kuliah")
public class JadwalKuliahMahasiswaController {

    private final JadwalKuliahMahasiswaService jadwalKuliahService;

    public JadwalKuliahMahasiswaController(JadwalKuliahMahasiswaService jadwalKuliahService) {
        this.jadwalKuliahService = jadwalKuliahService;
    }

    @GetMapping("/katalog")
    public ResponseEntity<KatalogJadwalKuliahResponseDTO> getKatalog() {
        return ResponseEntity.ok(jadwalKuliahService.getKatalog(requireNpm()));
    }

    @GetMapping
    public ResponseEntity<List<JadwalKuliahSayaItemDTO>> getJadwalSaya() {
        return ResponseEntity.ok(jadwalKuliahService.getJadwalSaya(requireNpm()));
    }

    @PostMapping
    public ResponseEntity<JadwalKuliahActionResponseDTO> tambahKelas(
        @RequestBody(required = false) PilihKelasRequestDTO request
    ) {
        return toResponse(jadwalKuliahService.tambahKelas(requireNpm(), request));
    }

    @DeleteMapping("/{idKelas}")
    public ResponseEntity<JadwalKuliahActionResponseDTO> hapusKelas(@PathVariable("idKelas") Integer idKelas) {
        return toResponse(jadwalKuliahService.hapusKelas(requireNpm(), idKelas));
    }

    private static ResponseEntity<JadwalKuliahActionResponseDTO> toResponse(JadwalKuliahActionResponseDTO response) {
        if (!response.success()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    private static String requireNpm() {
        String npm = AuthenticatedUserHelper.resolveMahasiswaNpm();
        if (npm == null || npm.isBlank()) {
            throw new IllegalArgumentException("Sesi tidak valid. Silakan login ulang.");
        }
        return npm;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
