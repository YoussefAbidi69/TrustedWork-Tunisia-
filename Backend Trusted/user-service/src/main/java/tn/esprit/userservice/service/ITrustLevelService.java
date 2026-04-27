package tn.esprit.userservice.service;

import tn.esprit.userservice.entity.User;

public interface ITrustLevelService {

    /**
     * Calcule et sauvegarde le Trust Level d'un utilisateur.
     * Algorithme SRS :
     * Trust Level = (KYC_validated x 40) + (2FA_enabled x 20)
     *             + (Liveness_passed x 30) + (Account_age_score x 10)
     */
    int computeAndSave(User user);

    /**
     * Retourne le Trust Level actuel d'un utilisateur par son ID.
     */
    int getTrustLevel(Long userId);

    /**
     * Recalcule le Trust Level de tous les utilisateurs actifs.
     * Appelé par le scheduler périodique.
     */
    void recomputeAll();
}