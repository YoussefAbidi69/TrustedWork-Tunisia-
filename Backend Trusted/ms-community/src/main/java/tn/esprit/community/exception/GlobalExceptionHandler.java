package tn.esprit.community.exception;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePostNotFound(PostNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(DownloadNotAllowedException.class)
    public ResponseEntity<ApiErrorResponse> handleDownloadNotAllowed(DownloadNotAllowedException ex) {
        return build(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(UpstreamCourseFileException.class)
    public ResponseEntity<ApiErrorResponse> handleUpstreamCourseFile(UpstreamCourseFileException ex) {
        return build(HttpStatus.BAD_GATEWAY, ex);
    }

    @ExceptionHandler(PostDeleteForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handlePostDeleteForbidden(PostDeleteForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(LearningNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleLearningNotFound(LearningNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    }

    @ExceptionHandler(ReportException.class)
    public ResponseEntity<ApiErrorResponse> handleReport(ReportException ex) {
        return build(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleDefault(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, Exception ex) {
        return ResponseEntity.status(status).body(ApiErrorResponse.builder()
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build());
    }
}
