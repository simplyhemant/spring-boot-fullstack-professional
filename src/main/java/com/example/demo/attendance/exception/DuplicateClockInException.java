package com.example.demo.attendance.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateClockInException extends RuntimeException {
    public DuplicateClockInException(String message) {
        super(message);
    }
}
