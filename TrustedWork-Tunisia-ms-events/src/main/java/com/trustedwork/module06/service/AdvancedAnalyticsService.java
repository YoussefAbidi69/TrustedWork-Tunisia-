package com.trustedwork.module06.service;

import com.trustedwork.module06.dto.GrowthProfileDTO;
import java.util.List;
import java.util.Map;

public interface AdvancedAnalyticsService {
    double computeInfluenceScore(Long userId);
    double predictChurnRisk(Long userId);
    Map<String, Object> getAiRecommendations(Long userId);
}
