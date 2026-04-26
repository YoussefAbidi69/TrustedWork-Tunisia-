package tn.esprit.userservice.service;

import tn.esprit.userservice.dto.AuthResponse;
import tn.esprit.userservice.dto.CompleteGoogleProfileRequest;

public interface IGoogleProfileService {

    /**
     * Retourne true si le profil Google de cet utilisateur est encore incomplet
     * (CIN temporaire négatif ou téléphone manquant).
     */
    boolean isProfileIncomplete(String email);

    /**
     * Complète le profil d'un utilisateur Google avec son CIN réel, téléphone et rôle.
     * Retourne un nouveau AuthResponse avec des tokens JWT rafraîchis
     * (le rôle peut avoir changé).
     */
    AuthResponse completeProfile(String email, CompleteGoogleProfileRequest request);
}
