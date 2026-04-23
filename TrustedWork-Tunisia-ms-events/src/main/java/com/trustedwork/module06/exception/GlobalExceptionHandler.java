package com.trustedwork.module06.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String MESSAGE_KEY = "message";
    private static final String TIMESTAMP_KEY = "timestamp";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Resource not found";
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of(MESSAGE_KEY, msg, TIMESTAMP_KEY, LocalDateTime.now()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Unauthorized";
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of(MESSAGE_KEY, msg, TIMESTAMP_KEY, LocalDateTime.now()));
    }

    @ExceptionHandler(AlreadyRegisteredException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyRegistered(AlreadyRegisteredException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Already registered";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of(MESSAGE_KEY, msg, TIMESTAMP_KEY, LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of(MESSAGE_KEY, "Internal server error", TIMESTAMP_KEY, LocalDateTime.now()));
    }
}
