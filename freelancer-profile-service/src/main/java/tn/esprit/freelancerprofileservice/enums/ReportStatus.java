package tn.esprit.freelancerprofileservice.enums;

/**
 * Statut d'un signalement de profil
 * Workflow : PENDING → REVIEWED → RESOLVED ou REJECTED
 */
public enum ReportStatus {
    PENDING,   // En attente de traitement admin
    IN_REVIEW,  // Examiné par un modérateur
    RESOLVED,  // Résolu (action prise)
    REJECTED   // Rejeté (signalement non fondé)
}