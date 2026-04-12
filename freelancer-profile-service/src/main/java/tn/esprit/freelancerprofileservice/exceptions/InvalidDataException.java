package tn.esprit.freelancerprofileservice.exceptions;

/**
 * Exception levée pour des données métier invalides (400)
 * Ex : note hors intervalle 1-5, auto-endorsement
 */
public class InvalidDataException extends RuntimeException {

    public InvalidDataException(String message) {
        super(message);
    }
}