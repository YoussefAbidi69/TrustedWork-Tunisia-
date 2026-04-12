package tn.esprit.mscontractservicee.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        log.warn("Request failed [{} {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getReason());
        return buildResponse(ex.getReason() != null ? ex.getReason() : "Request failed", status);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeign(FeignException ex, HttpServletRequest request) {
        log.warn("Downstream error [{} {}]: status={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.status(),
                ex.getMessage());
        return buildResponse("Downstream service error: " + ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Invalid request payload [{} {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
        return buildResponse("Invalid request payload. Check field types and date formats (LocalDate: YYYY-MM-DD, LocalDateTime: YYYY-MM-DDTHH:mm:ss).", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex, HttpServletRequest request) {
        log.warn("Runtime error [{} {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());
        return buildResponse(ex.getMessage() != null ? ex.getMessage() : "Bad request", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error [{} {}]", request.getMethod(), request.getRequestURI(), ex);
        return buildResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", message);
        return new ResponseEntity<>(response, status);
    }
}
