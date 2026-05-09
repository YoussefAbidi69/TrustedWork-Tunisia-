package tn.esprit.freelancerprofileservice.exceptions;

/**
 * Exception levée lors d'une tentative de création dupliquée (409)
 * Ex : profil déjà existant, endorsement déjà soumis, avis déjà laissé
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}