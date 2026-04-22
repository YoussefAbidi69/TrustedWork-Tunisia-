package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.entity.GrowthProfile;
import com.trustedwork.module06.entity.Streak;
import com.trustedwork.module06.repository.EventRepository;
import com.trustedwork.module06.repository.GrowthProfileRepository;
import com.trustedwork.module06.repository.StreakRepository;
import com.trustedwork.module06.repository.UserBadgeRepository;
import com.trustedwork.module06.service.AdvancedAnalyticsService;
import com.trustedwork.module06.service.AiRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdvancedAnalyticsServiceImpl implements AdvancedAnalyticsService {

    private final GrowthProfileRepository growthRepo;
    private final UserBadgeRepository userBadgeRepo;
    private final StreakRepository streakRepo;
    private final AiRecommendationService aiRecommendationService;

    @Override
    public double computeInfluenceScore(Long userId) {
        // Influence = (Badges Count * 50) + (XP * 0.1) + (Streak * 20)
        long badgeCount = userBadgeRepo.findByUserId(userId).size();
        int xp = growthRepo.findByUserId(userId).map(GrowthProfile::getXpPoints).orElse(0);
        int streak = streakRepo.findByUserId(userId).map(Streak::getCurrentStreak).orElse(0);

        double score = (badgeCount * 50.0) + (xp * 0.1) + (streak * 20.0);
        return Math.round(score * 100.0) / 100.0;
    }

    @Override
    public double predictChurnRisk(Long userId) {
        // Risk based on inactivity days
        LocalDate lastActivity = streakRepo.findByUserId(userId)
                .map(Streak::getLastActivityDate)
                .orElse(LocalDate.now().minusDays(30));

        long daysInactive = ChronoUnit.DAYS.between(lastActivity, LocalDate.now());
        
        // 0 risk if active today, caps at 1.0 (100%) at 30 days
        double risk = Math.min(daysInactive / 30.0, 1.0);
        return Math.round(risk * 100.0) / 100.0;
    }

    @Override
    public Map<String, Object> getAiRecommendations(Long userId) {
        return aiRecommendationService.getSmartRecommendations(userId);
    }
}
