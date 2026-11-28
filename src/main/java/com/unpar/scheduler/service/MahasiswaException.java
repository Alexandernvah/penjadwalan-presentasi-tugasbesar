package com.unpar.scheduler.service;

import org.springframework.http.HttpStatus;

public class MahasiswaException extends RuntimeException {

    private final HttpStatus status;

    public MahasiswaException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
