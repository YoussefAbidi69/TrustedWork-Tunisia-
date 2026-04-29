package tn.esprit.smartjobboard.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("EntityNotFoundException → 404")
    void entityNotFound() {
        ResponseEntity<Map<String, Object>> resp = handler.notFound(
                new EntityNotFoundException("Job offer not found: 42"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).containsEntry("status", 404);
        assertThat(resp.getBody().get("message")).asString().contains("42");
    }

    @Test
    @DisplayName("DuplicateApplicationException → 409")
    void duplicateApplication() {
        ResponseEntity<Map<String, Object>> resp = handler.conflict(
                new DuplicateApplicationException("Already applied"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).containsEntry("status", 409);
    }

    @Test
    @DisplayName("InvalidStatusTransitionException → 409")
    void invalidTransition() {
        ResponseEntity<Map<String, Object>> resp = handler.conflict(
                new InvalidStatusTransitionException("Cannot transition"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("JobNotEditableException → 409")
    void jobNotEditable() {
        ResponseEntity<Map<String, Object>> resp = handler.conflict(
                new JobNotEditableException("Not editable"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("JobClosedException → 403")
    void jobClosed() {
        ResponseEntity<Map<String, Object>> resp = handler.jobClosed(
                new JobClosedException("Job is closed"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).containsEntry("error", "Forbidden");
    }

    @Test
    @DisplayName("ForbiddenOperationException → 403")
    void forbiddenOp() {
        ResponseEntity<Map<String, Object>> resp = handler.forbiddenOp(
                new ForbiddenOperationException("Access denied"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("AccessDeniedException with message → 403 with message")
    void accessDeniedWithMessage() {
        ResponseEntity<Map<String, Object>> resp = handler.accessDenied(
                new AccessDeniedException("Custom deny message"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().get("message")).asString().contains("Custom deny message");
    }

    @Test
    @DisplayName("AccessDeniedException with blank message → 403 with default message")
    void accessDeniedBlankMessage() {
        ResponseEntity<Map<String, Object>> resp = handler.accessDenied(
                new AccessDeniedException(""));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().get("message")).asString().contains("permission");
    }

    @Test
    @DisplayName("IllegalArgumentException → 400")
    void badRequest() {
        ResponseEntity<Map<String, Object>> resp = handler.badRequest(
                new IllegalArgumentException("Invalid input"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("RestClientException → 503")
    void serviceUnavailable() {
        ResponseEntity<Map<String, Object>> resp = handler.upstreamUserService(
                new RestClientException("Connection refused"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(resp.getBody().get("message")).asString().contains("user-service");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 with field details")
    void validationError() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fe1 = new FieldError("req", "coverLetter", "must not be blank");
        FieldError fe2 = new FieldError("req", "proposedRate", "must be positive");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fe1, fe2));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<Map<String, Object>> resp = handler.validation(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().get("message")).asString()
                .contains("coverLetter")
                .contains("proposedRate");
    }

    @Test
    @DisplayName("ConstraintViolationException → 400")
    void constraintViolation() {
        ConstraintViolationException ex = new ConstraintViolationException("size must be between 10 and 8000", Set.of());
        ResponseEntity<Map<String, Object>> resp = handler.constraint(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Generic Exception → 500")
    void fallback() {
        ResponseEntity<Map<String, Object>> resp = handler.fallback(
                new RuntimeException("Unexpected error"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody()).containsEntry("error", "Internal Server Error");
    }

    @Test
    @DisplayName("Response body should contain timestamp field")
    void timestampPresent() {
        ResponseEntity<Map<String, Object>> resp = handler.notFound(
                new EntityNotFoundException("test"));

        assertThat(resp.getBody()).containsKey("timestamp");
        assertThat(resp.getBody().get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("Should handle null message gracefully")
    void nullMessage() {
        ResponseEntity<Map<String, Object>> resp = handler.fallback(new RuntimeException((String) null));

        assertThat(resp.getBody().get("message")).isEqualTo("");
    }
}
