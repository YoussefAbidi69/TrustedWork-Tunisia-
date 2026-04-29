package tn.esprit.smartjobboard.exception;

public class JobClosedException extends RuntimeException {
    public JobClosedException(String message) {
        super(message);
    }
}
