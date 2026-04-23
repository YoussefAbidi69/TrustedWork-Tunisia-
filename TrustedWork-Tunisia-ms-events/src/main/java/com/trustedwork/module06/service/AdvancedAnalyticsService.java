package com.trustedwork.module06.service;

import java.util.Map;

public interface AdvancedAnalyticsService {

    /** Score d'influence basé sur XP, Badges et Streak */
    double computeInfluenceScore(Long userId);

    /** Risque de churn simple (formule mathématique — conservé pour compatibilité) */
    double predictChurnRisk(Long userId);

    /** Prédiction ML complète via le modèle Random Forest Python */
    Map<String, Object> getChurnPrediction(Long userId);

    /** Métriques académiques du modèle entraîné (accuracy, F1, ROC-AUC...) */
    Map<String, Object> getModelStats();

    /** Recommandations IA via Groq (déjà existant — intact) */
    Map<String, Object> getAiRecommendations(Long userId);
}
