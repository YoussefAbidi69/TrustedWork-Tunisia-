package com.trustedwork.module06.service;

import java.util.Map;

public interface AiRecommendationService {
    
    /**
     * Analyse le profil utilisateur et suggère les meilleurs événements et missions.
     */
    Map<String, Object> getSmartRecommendations(Long userId);
}
