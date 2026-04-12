package tn.esprit.freelancerprofileservice.services;

public interface ISkillAuthenticityService {

    // Calculer et sauvegarder le score d'authenticité d'un skill
    double calculateAuthenticityScore(Long skillId);

    // Recalculer tous les scores d'un profil (appelé par le scheduler)
    void recalculateAllScores(Long profileId);
}