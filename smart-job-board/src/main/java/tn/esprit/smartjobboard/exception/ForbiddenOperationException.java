package tn.esprit.smartjobboard.exception;

/**
 * Domain-level 403 when business rules forbid an action (distinct from Spring Security access denied).
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
