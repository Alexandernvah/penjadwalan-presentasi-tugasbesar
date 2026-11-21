package com.unpar.scheduler.service;

import com.unpar.scheduler.dto.penjadwalan.CekBentrokResponseDTO;

public class ConflictScheduleException extends RuntimeException {

    private final CekBentrokResponseDTO conflictResponse;

    public ConflictScheduleException(String message, CekBentrokResponseDTO conflictResponse) {
        super(message);
        this.conflictResponse = conflictResponse;
    }

    public CekBentrokResponseDTO getConflictResponse() {
        return conflictResponse;
    }
}
