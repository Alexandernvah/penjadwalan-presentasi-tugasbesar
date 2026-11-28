package com.unpar.scheduler.controller;

import com.unpar.scheduler.service.MahasiswaException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Satu bentuk JSON {"message": ...} untuk semua galat portal mahasiswa.
@RestControllerAdvice(assignableTypes = {MahasiswaMatkulController.class, MahasiswaDashboardController.class})
public class MahasiswaExceptionHandler {

    @ExceptionHandler(MahasiswaException.class)
    public ResponseEntity<Map<String, String>> handleMahasiswaException(MahasiswaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody() {
        return ResponseEntity.badRequest().body(Map.of("message", "Format permintaan tidak valid."));
    }

    // Batas unik (npm, id_kelas) di database sebagai pengaman terakhir bila dua submit lolos bersamaan.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEnrollment() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("message", "Mata kuliah telah dikunci. Hubungi Admin Program Studi jika terdapat kesalahan pengisian data."));
    }
}
