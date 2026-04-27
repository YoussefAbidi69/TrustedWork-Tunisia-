package tn.esprit.userservice.service;

import tn.esprit.userservice.dto.AuthResponse;
import tn.esprit.userservice.dto.GoogleOAuthRequest;

public interface IGoogleOAuthService {

    /**
     * Vérifie le credential Google (ID Token), crée ou retrouve l'utilisateur,
     * et retourne un AuthResponse avec les tokens JWT de l'application.
     */
    AuthResponse loginWithGoogle(GoogleOAuthRequest request);
}
