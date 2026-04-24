package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.entity.GrowthProfile;
import com.trustedwork.module06.entity.Streak;
import com.trustedwork.module06.repository.ChallengeParticipationRepository;
import com.trustedwork.module06.repository.EventRegistrationRepository;
import com.trustedwork.module06.repository.GrowthProfileRepository;
import com.trustedwork.module06.repository.StreakRepository;
import com.trustedwork.module06.repository.UserBadgeRepository;
import com.trustedwork.module06.service.MlPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Implémentation du service ML.
 * Collecte les features réelles de l'utilisateur depuis la base de données
 * et appelle le microservice Python Flask (Random Forest) pour la prédiction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MlPredictionServiceImpl implements MlPredictionService {

    private final GrowthProfileRepository growthRepo;
    private final StreakRepository streakRepo;
    private final UserBadgeRepository userBadgeRepo;
    private final EventRegistrationRepository eventRegistrationRepo;
    private final ChallengeParticipationRepository challengeParticipationRepo;
    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:5001}")
    private String aiServiceUrl;

    // ─────────────────────────────────────────────
    // Prédiction du risque de churn
    // ─────────────────────────────────────────────
    @Override
    public Map<String, Object> predictChurnRisk(Long userId) {
        log.info("[ML] Début prédiction churn pour userId={}", userId);

        Map<String, Object> features = buildFeatureVector(userId);
        log.debug("[ML] Vecteur de features : {}", features);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(features, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    aiServiceUrl + "/predict/churn",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("[ML] Prédiction reçue pour userId={} : {}", userId, response.getBody());
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("[ML] Service Python indisponible ({}). Mode fallback activé.", e.getMessage());
        }

        // Mode fallback : calcul local si le service Python est down
        return buildFallbackResponse(userId, features);
    }

    // ─────────────────────────────────────────────
    // Métriques académiques du modèle
    // ─────────────────────────────────────────────
    @Override
    public Map<String, Object> getModelStats() {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    aiServiceUrl + "/model/stats",
                    HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("[ML] Impossible de récupérer les stats du modèle : {}", e.getMessage());
        }
        return Map.of("error", "Service ML indisponible", "status", "DOWN");
    }

    // ─────────────────────────────────────────────
    // Health check du microservice Python
    // ─────────────────────────────────────────────
    @Override
    public boolean isServiceAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    aiServiceUrl + "/health",
                    String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("[ML] Service Python non disponible : {}", e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────
    // Construction du vecteur de features
    // ─────────────────────────────────────────────
    private Map<String, Object> buildFeatureVector(Long userId) {
        // 1. GrowthProfile
        GrowthProfile profile = growthRepo.findByUserId(userId).orElse(null);
        int xpPoints = profile != null ? profile.getXpPoints() : 0;
        int level = profile != null ? profile.getLevel() : 1;
        double engagementScore = profile != null ? profile.getEngagementScore() : 0.0;

        // 2. Streak
        Streak streak = streakRepo.findByUserId(userId).orElse(null);
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int longestStreak = streak != null ? streak.getLongestStreak() : 0;

        long daysInactive = 0;
        if (streak != null && streak.getLastActivityDate() != null) {
            daysInactive = ChronoUnit.DAYS.between(streak.getLastActivityDate(), LocalDate.now());
        } else {
            daysInactive = 30; // Inconnu → on suppose inactif depuis 30j
        }

        // 3. Badges
        int badgesCount = userBadgeRepo.findByUserId(userId).size();

        // 4. Events attendus
        int eventsAttended = (int) eventRegistrationRepo.countByUserId(userId);

        // 5. Challenges complétés
        int challengesCompleted = challengeParticipationRepo.findByUserId(userId).size();

        Map<String, Object> features = new HashMap<>();
        features.put("user_id", userId);
        features.put("xp_points", xpPoints);
        features.put("level", level);
        features.put("engagement_score", engagementScore);
        features.put("current_streak", currentStreak);
        features.put("longest_streak", longestStreak);
        features.put("days_inactive", daysInactive);
        features.put("badges_count", badgesCount);
        features.put("events_attended", eventsAttended);
        features.put("challenges_completed", challengesCompleted);

        return features;
    }

    // ─────────────────────────────────────────────
    // Fallback local si Python est down
    // ─────────────────────────────────────────────
    private Map<String, Object> buildFallbackResponse(Long userId, Map<String, Object> features) {
        long daysInactive = features.get("days_inactive") instanceof Number n
                ? n.longValue() : 30L;
        int currentStreak = features.get("current_streak") instanceof Number n
                ? n.intValue() : 0;
        double engagementScore = features.get("engagement_score") instanceof Number n
                ? n.doubleValue() : 0.0;

        double riskScore = 0;
        if (daysInactive >= 14) riskScore += 0.4;
        else if (daysInactive >= 7) riskScore += 0.25;
        else if (daysInactive >= 3) riskScore += 0.1;

        if (currentStreak == 0) riskScore += 0.3;
        if (engagementScore < 30) riskScore += 0.2;

        double churnProbability = Math.min(riskScore * 100, 99.0);
        boolean churnPredicted = churnProbability >= 50;

        String riskLabel;
        String recommendation;
        if (churnProbability >= 70) {
            riskLabel = "HIGH";
            recommendation = "Attention : Votre engagement baisse. Relevez un nouveau défi pour booster votre profil !";
        } else if (churnProbability >= 40) {
            riskLabel = "MEDIUM";
            recommendation = "Pas mal ! Pourquoi ne pas participer à un événement pour augmenter votre score ?";
        } else {
            riskLabel = "LOW";
            recommendation = "Excellent ! Continuez ainsi pour maintenir votre série d'activité.";
        }

        Map<String, Object> fallback = new HashMap<>();
        fallback.put("user_id", userId);
        fallback.put("churn_predicted", churnPredicted);
        fallback.put("churn_probability", Math.round(churnProbability * 100.0) / 100.0);
        fallback.put("risk_label", riskLabel);
        fallback.put("recommendation", recommendation);
        fallback.put("model", "Fallback (Python service down)");
        return fallback;
    }
}
