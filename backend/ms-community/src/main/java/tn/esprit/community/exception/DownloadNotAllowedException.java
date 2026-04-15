package tn.esprit.community.exception;

public class DownloadNotAllowedException extends RuntimeException {
    public DownloadNotAllowedException(String message) {
        super(message);
    }
}
