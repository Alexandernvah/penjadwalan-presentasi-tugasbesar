package com.unpar.scheduler.controller;

import com.unpar.scheduler.dto.jadwalpribadi.JadwalPribadiDosenResponseDTO;
import com.unpar.scheduler.service.JadwalPribadiDosenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dosen/jadwal-pribadi")
public class JadwalPribadiDosenController {

    private final JadwalPribadiDosenService jadwalPribadiDosenService;

    public JadwalPribadiDosenController(JadwalPribadiDosenService jadwalPribadiDosenService) {
        this.jadwalPribadiDosenService = jadwalPribadiDosenService;
    }

    @GetMapping
    public ResponseEntity<JadwalPribadiDosenResponseDTO> getJadwalPribadi(
        @RequestParam(name = "dosenId", required = false) String dosenId
    ) {
        return ResponseEntity.ok(jadwalPribadiDosenService.getJadwalPribadi(dosenId));
    }
}
