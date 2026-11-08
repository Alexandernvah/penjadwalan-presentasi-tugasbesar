package com.unpar.scheduler.controller;

import com.unpar.scheduler.dto.kelompok.BatchCreateKelompokRequestDTO;
import com.unpar.scheduler.dto.kelompok.CreateKelompokRequestDTO;
import com.unpar.scheduler.dto.kelompok.KelompokDetailDTO;
import com.unpar.scheduler.service.KelompokService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dosen/kelompok")
public class KelompokController {

    private final KelompokService kelompokService;

    public KelompokController(KelompokService kelompokService) {
        this.kelompokService = kelompokService;
    }

    @GetMapping
    public ResponseEntity<List<KelompokDetailDTO>> getKelompokByKelas(
        @RequestParam(name = "kelasId", required = false) Integer kelasId
    ) {
        List<KelompokDetailDTO> list = kelompokService.getKelompokByKelas(kelasId);
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<KelompokDetailDTO> createKelompok(
        @RequestBody CreateKelompokRequestDTO request
    ) {
        KelompokDetailDTO created = kelompokService.createKelompok(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<KelompokDetailDTO>> batchCreateKelompok(
        @RequestBody BatchCreateKelompokRequestDTO request
    ) {
        List<KelompokDetailDTO> list = kelompokService.batchCreateKelompok(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(list);
    }
}

