package tn.esprit.userservice.service;

import tn.esprit.userservice.dto.PublicUserDTO;
import tn.esprit.userservice.dto.TokenValidationDTO;

public interface IIdentityService {

    /**
     * Valide un JWT et retourne les données utilisateur extraites.
     * Retourne valid=false si le token est expiré, invalide ou révoqué.
     */
    TokenValidationDTO validateToken(String authHeader);

    /**
     * Retourne le profil public minimal d'un utilisateur.
     */
    PublicUserDTO getPublicProfile(Long userId);

    /**
     * Retourne les infos Trust Level d'un utilisateur.
     */
    TokenValidationDTO getTrustLevelInfo(Long userId);
}