package com.unpar.scheduler.controller;

import com.unpar.scheduler.config.AuthenticatedUserHelper;
import com.unpar.scheduler.dto.jadwalmengawas.JadwalMengawasActionResponseDTO;
import com.unpar.scheduler.dto.jadwalmengawas.JadwalMengawasResponseDTO;
import com.unpar.scheduler.dto.jadwalmengawas.PilihMengawasRequestDTO;
import com.unpar.scheduler.dto.jadwalmengawas.PindahMengawasRequestDTO;
import com.unpar.scheduler.service.JadwalMengawasService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dosen/jadwal-mengawas")
public class JadwalMengawasController {

    private final JadwalMengawasService jadwalMengawasService;

    public JadwalMengawasController(JadwalMengawasService jadwalMengawasService) {
        this.jadwalMengawasService = jadwalMengawasService;
    }

    @GetMapping
    public ResponseEntity<JadwalMengawasResponseDTO> getJadwalMengawas(
        @RequestParam(name = "dosenId", required = false) String dosenId
    ) {
        String resolvedDosenId = AuthenticatedUserHelper.resolveDosenNidn(dosenId);
        return ResponseEntity.ok(jadwalMengawasService.getJadwalMengawas(resolvedDosenId));
    }

    @PostMapping
    public ResponseEntity<JadwalMengawasActionResponseDTO> pilihJadwalMengawas(
        @RequestParam(name = "dosenId", required = false) String dosenId,
        @RequestBody(required = false) PilihMengawasRequestDTO request
    ) {
        String resolvedDosenId = AuthenticatedUserHelper.resolveDosenNidn(dosenId);
        JadwalMengawasActionResponseDTO response = jadwalMengawasService.pilihJadwalMengawas(resolvedDosenId, request);
        if (response.conflict()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{idUjian}")
    public ResponseEntity<JadwalMengawasActionResponseDTO> batalkanJadwalMengawas(
        @PathVariable("idUjian") Integer idUjian,
        @RequestParam(name = "dosenId", required = false) String dosenId
    ) {
        String resolvedDosenId = AuthenticatedUserHelper.resolveDosenNidn(dosenId);
        JadwalMengawasActionResponseDTO response = jadwalMengawasService.batalkanJadwalMengawas(resolvedDosenId, idUjian);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pindah")
    public ResponseEntity<JadwalMengawasActionResponseDTO> pindahJadwalMengawas(
        @RequestParam(name = "dosenId", required = false) String dosenId,
        @RequestBody(required = false) PindahMengawasRequestDTO request
    ) {
        String resolvedDosenId = AuthenticatedUserHelper.resolveDosenNidn(dosenId);
        JadwalMengawasActionResponseDTO response = jadwalMengawasService.pindahJadwalMengawas(resolvedDosenId, request);
        if (response.conflict()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
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
