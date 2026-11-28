package com.unpar.scheduler.controller;

import com.unpar.scheduler.service.MahasiswaException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

// Satu bentuk JSON {"message": ...} untuk semua galat portal mahasiswa.
@RestControllerAdvice(assignableTypes = {
    MahasiswaMatkulController.class,
    MahasiswaDashboardController.class,
    MahasiswaKelompokController.class,
    MahasiswaJadwalController.class
})
public class MahasiswaExceptionHandler {

    @ExceptionHandler(MahasiswaException.class)
    public ResponseEntity<Map<String, String>> handleMahasiswaException(MahasiswaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> handleInvalidRequest() {
        return ResponseEntity.badRequest().body(Map.of("message", "Format permintaan tidak valid."));
    }

    // Batas unik di database sebagai pengaman terakhir bila dua request lolos bersamaan.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateConstraint(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
        if (detail != null && detail.contains("uq_anggota_kelas")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Anda sudah tergabung dalam kelompok pada kelas ini."));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("message", "Mata kuliah telah dikunci. Hubungi Admin Program Studi jika terdapat kesalahan pengisian data."));
    }
}
