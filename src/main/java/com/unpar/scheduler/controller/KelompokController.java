package com.unpar.scheduler.controller;

import com.unpar.scheduler.dto.kelompok.BatchCreateKelompokRequestDTO;
import com.unpar.scheduler.dto.kelompok.CreateKelompokRequestDTO;
import com.unpar.scheduler.dto.kelompok.KelompokDetailDTO;
import com.unpar.scheduler.dto.kelompok.UpdateKelompokRequestDTO;
import com.unpar.scheduler.service.KelompokService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    @PutMapping("/{id}")
    public ResponseEntity<KelompokDetailDTO> updateKelompok(
        @PathVariable("id") Integer id,
        @RequestBody UpdateKelompokRequestDTO request
    ) {
        KelompokDetailDTO updated = kelompokService.updateKelompok(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKelompok(
        @PathVariable("id") Integer id
    ) {
        kelompokService.deleteKelompok(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}

