package tn.esprit.community.exception;

public class PostDeleteForbiddenException extends RuntimeException {
    public PostDeleteForbiddenException(String message) {
        super(message);
    }
}
