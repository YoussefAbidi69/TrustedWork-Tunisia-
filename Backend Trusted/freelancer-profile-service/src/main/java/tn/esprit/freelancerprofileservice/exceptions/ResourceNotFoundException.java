package tn.esprit.freelancerprofileservice.exceptions;

/**
 * Exception levée quand une ressource est introuvable en base (404)
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " introuvable avec l'id : " + id);
    }
}