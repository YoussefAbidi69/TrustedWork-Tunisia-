package com.trustedwork.module06.service;

import java.util.Map;

/**
 * Service de communication avec le microservice Python IA (Flask - port 5001).
 * Appelle le modèle Random Forest entraîné pour prédire le risque de churn.
 */
public interface MlPredictionService {

    /**
     * Construit le vecteur de features à partir des données réelles de l'utilisateur
     * (GrowthProfile, Streak, UserBadge, EventRegistration, ChallengeParticipation)
     * et appelle le microservice Python pour obtenir la prédiction.
     *
     * @param userId identifiant de l'utilisateur
     * @return Map contenant : churn_predicted, churn_probability, risk_label, recommendation
     */
    Map<String, Object> predictChurnRisk(Long userId);

    /**
     * Récupère les métriques académiques du modèle ML (accuracy, F1, ROC-AUC, etc.)
     *
     * @return Map contenant les statistiques du modèle entraîné
     */
    Map<String, Object> getModelStats();

    /**
     * Vérifie que le microservice Python est disponible.
     *
     * @return true si le service répond correctement
     */
    boolean isServiceAvailable();
}
