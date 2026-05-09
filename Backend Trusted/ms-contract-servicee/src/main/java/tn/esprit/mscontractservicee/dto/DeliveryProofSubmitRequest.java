package tn.esprit.mscontractservicee.dto;

/**
 * Payload fourni par le FREELANCER lors de la soumission d'un jalon.
 * Pour l'instant, le champ {@code fichiers} reste un String (ex: URLs séparées par des virgules)
 * car le projet ne gère pas encore d'upload multipart.
 */
public record DeliveryProofSubmitRequest(
        String fichiers,
        String lienDemo,
        String repoGit,
        String commentaire,
        String hashMD5
) {
}

