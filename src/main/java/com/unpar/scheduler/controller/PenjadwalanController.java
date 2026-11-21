package com.unpar.scheduler.controller;

import com.unpar.scheduler.dto.penjadwalan.CekBentrokRequestDTO;
import com.unpar.scheduler.dto.penjadwalan.CekBentrokResponseDTO;
import com.unpar.scheduler.dto.penjadwalan.KelasPenjadwalanDTO;
import com.unpar.scheduler.dto.penjadwalan.KelompokPenjadwalanDTO;
import com.unpar.scheduler.dto.penjadwalan.MatriksMingguanResponseDTO;
import com.unpar.scheduler.dto.penjadwalan.MatriksPenjadwalanResponseDTO;
import com.unpar.scheduler.dto.penjadwalan.PresentasiTerjadwalDTO;
import com.unpar.scheduler.dto.penjadwalan.RuanganOptionDTO;
import com.unpar.scheduler.dto.penjadwalan.SimpanPresentasiRequestDTO;
import com.unpar.scheduler.service.ConflictScheduleException;
import com.unpar.scheduler.service.PenjadwalanService;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dosen/penjadwalan")
public class PenjadwalanController {

    private final PenjadwalanService penjadwalanService;

    public PenjadwalanController(PenjadwalanService penjadwalanService) {
        this.penjadwalanService = penjadwalanService;
    }

    @GetMapping("/kelas")
    public ResponseEntity<List<KelasPenjadwalanDTO>> getDaftarKelas(
        @RequestParam(name = "dosenId", required = false) String dosenId
    ) {
        List<KelasPenjadwalanDTO> kelasList = penjadwalanService.getDaftarKelasDosen(dosenId);
        return ResponseEntity.ok(kelasList);
    }

    @GetMapping("/kelas/{idKelas}/kelompok")
    public ResponseEntity<List<KelompokPenjadwalanDTO>> getKelompokByKelas(
        @PathVariable("idKelas") Integer idKelas
    ) {
        List<KelompokPenjadwalanDTO> kelompokList = penjadwalanService.getKelompokByKelas(idKelas);
        return ResponseEntity.ok(kelompokList);
    }

    @GetMapping("/ruangan")
    public ResponseEntity<List<RuanganOptionDTO>> getDaftarRuangan() {
        List<RuanganOptionDTO> ruanganList = penjadwalanService.getDaftarRuangan();
        return ResponseEntity.ok(ruanganList);
    }

    @GetMapping("/matriks-mingguan")
    public ResponseEntity<MatriksMingguanResponseDTO> getMatriksMingguan(
        @RequestParam(name = "idKelas", required = false) Integer idKelas,
        @RequestParam(name = "idKelompok", required = false) Integer idKelompok,
        @RequestParam(name = "mode", required = false, defaultValue = "REGULER") String mode,
        @RequestParam(name = "tanggalMulai", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggalMulai,
        @RequestParam(name = "dosenId", required = false) String dosenId,
        @RequestParam(name = "durasiMenit", required = false) Integer durasiMenit
    ) {
        MatriksMingguanResponseDTO matriks = penjadwalanService.getMatriksMingguan(
            idKelas, idKelompok, mode, tanggalMulai, dosenId, durasiMenit
        );
        return ResponseEntity.ok(matriks);
    }

    @GetMapping("/matriks")
    public ResponseEntity<MatriksPenjadwalanResponseDTO> getMatriksPenjadwalan(
        @RequestParam(name = "idKelas", required = false) Integer idKelas,
        @RequestParam(name = "ruangan", required = false) String ruangan,
        @RequestParam(name = "tanggal", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggal,
        @RequestParam(name = "idKelompok", required = false) Integer idKelompok,
        @RequestParam(name = "dosenId", required = false) String dosenId
    ) {
        MatriksPenjadwalanResponseDTO matriks = penjadwalanService.getMatriksHarian(
            idKelas, ruangan, tanggal, idKelompok, dosenId
        );
        return ResponseEntity.ok(matriks);
    }

    @PostMapping("/cek-bentrok")
    public ResponseEntity<CekBentrokResponseDTO> cekBentrok(
        @RequestBody CekBentrokRequestDTO request
    ) {
        CekBentrokResponseDTO response = penjadwalanService.cekBentrok(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/presentasi")
    public ResponseEntity<PresentasiTerjadwalDTO> simpanPresentasi(
        @RequestBody SimpanPresentasiRequestDTO request
    ) {
        PresentasiTerjadwalDTO saved = penjadwalanService.simpanPresentasi(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/presentasi/{idPresentasi}")
    public ResponseEntity<Void> hapusPresentasi(
        @PathVariable("idPresentasi") Integer idPresentasi
    ) {
        penjadwalanService.hapusPresentasi(idPresentasi);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ConflictScheduleException.class)
    public ResponseEntity<Map<String, Object>> handleConflictScheduleException(ConflictScheduleException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        if (ex.getConflictResponse() != null) {
            body.put("adaBentrok", ex.getConflictResponse().adaBentrok());
            body.put("jamMulai", ex.getConflictResponse().jamMulai());
            body.put("jamSelesai", ex.getConflictResponse().jamSelesai());
            body.put("durasiMenit", ex.getConflictResponse().durasiMenit());
            body.put("konflikList", ex.getConflictResponse().konflikList());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
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
