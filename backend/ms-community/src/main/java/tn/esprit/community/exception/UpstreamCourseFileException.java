package tn.esprit.community.exception;

/**
 * The post's {@code fileUrl} could not be fetched (wrong URL, upstream 404, connection refused, etc.).
 */
public class UpstreamCourseFileException extends RuntimeException {

    public UpstreamCourseFileException(String message) {
        super(message);
    }

    public UpstreamCourseFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
