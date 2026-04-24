package tn.esprit.freelancerprofileservice.exceptions;

/**
 * Exception levée quand un utilisateur tente une action sur une ressource
 * qui ne lui appartient pas (403)
 */
public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}