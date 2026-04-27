package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.entity.GrowthProfile;
import com.trustedwork.module06.entity.Streak;
import com.trustedwork.module06.repository.ChallengeParticipationRepository;
import com.trustedwork.module06.repository.EventRegistrationRepository;
import com.trustedwork.module06.repository.GrowthProfileRepository;
import com.trustedwork.module06.repository.StreakRepository;
import com.trustedwork.module06.repository.UserBadgeRepository;
import com.trustedwork.module06.service.AdvancedAnalyticsService;
import com.trustedwork.module06.service.AiRecommendationService;
import com.trustedwork.module06.service.MlPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdvancedAnalyticsServiceImpl implements AdvancedAnalyticsService {

    private final GrowthProfileRepository growthRepo;
    private final UserBadgeRepository userBadgeRepo;
    private final StreakRepository streakRepo;
    private final EventRegistrationRepository eventRegistrationRepo;
    private final ChallengeParticipationRepository challengeParticipationRepo;
    private final AiRecommendationService aiRecommendationService;
    private final MlPredictionService mlPredictionService;

    // ─────────────────────────────────────────────────────────────────────────
    // Influence Score : formule basée sur Badges, XP et Streak
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public double computeInfluenceScore(Long userId) {
        long badgeCount = userBadgeRepo.findByUserId(userId).size();
        int xp = growthRepo.findByUserId(userId).map(GrowthProfile::getXpPoints).orElse(0);
        int streak = streakRepo.findByUserId(userId).map(Streak::getCurrentStreak).orElse(0);
        long eventsCount = eventRegistrationRepo.countByUserId(userId);
        long challengesCount = challengeParticipationRepo.findByUserId(userId).size();

        double score = (badgeCount * 50.0) + (xp * 0.1) + (streak * 20.0) 
                     + (eventsCount * 30.0) + (challengesCount * 40.0);
        
        return Math.round(score * 100.0) / 100.0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Churn Risk simple (formule mathématique — conservé pour compatibilité)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public double predictChurnRisk(Long userId) {
        LocalDate lastActivity = streakRepo.findByUserId(userId)
                .map(Streak::getLastActivityDate)
                .orElse(LocalDate.now().minusDays(30));

        long daysInactive = ChronoUnit.DAYS.between(lastActivity, LocalDate.now());
        double risk = Math.min(daysInactive / 30.0, 1.0);
        return Math.round(risk * 100.0) / 100.0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prédiction ML complète via le modèle Random Forest Python (NOUVEAU)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public Map<String, Object> getChurnPrediction(Long userId) {
        return mlPredictionService.predictChurnRisk(userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Métriques académiques du modèle entraîné (NOUVEAU)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public Map<String, Object> getModelStats() {
        return mlPredictionService.getModelStats();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recommandations Groq (inchangé)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public Map<String, Object> getAiRecommendations(Long userId) {
        return aiRecommendationService.getSmartRecommendations(userId);
    }
}

