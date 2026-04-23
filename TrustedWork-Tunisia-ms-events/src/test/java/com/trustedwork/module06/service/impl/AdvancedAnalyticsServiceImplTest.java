package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.entity.GrowthProfile;
import com.trustedwork.module06.entity.Streak;
import com.trustedwork.module06.repository.GrowthProfileRepository;
import com.trustedwork.module06.repository.StreakRepository;
import com.trustedwork.module06.repository.UserBadgeRepository;
import com.trustedwork.module06.service.AiRecommendationService;
import com.trustedwork.module06.service.MlPredictionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvancedAnalyticsServiceImplTest {

    @Mock
    private GrowthProfileRepository growthRepo;
    @Mock
    private UserBadgeRepository userBadgeRepo;
    @Mock
    private StreakRepository streakRepo;
    @Mock
    private AiRecommendationService aiRecommendationService;
    @Mock
    private MlPredictionService mlPredictionService;

    @InjectMocks
    private AdvancedAnalyticsServiceImpl advancedAnalyticsService;

    @Test
    void testComputeInfluenceScore() {
        Long userId = 1L;
        when(userBadgeRepo.findByUserId(userId)).thenReturn(Collections.nCopies(3, null));
        when(growthRepo.findByUserId(userId)).thenReturn(Optional.of(GrowthProfile.builder().xpPoints(1000).build()));
        when(streakRepo.findByUserId(userId)).thenReturn(Optional.of(Streak.builder().currentStreak(5).build()));

        double result = advancedAnalyticsService.computeInfluenceScore(userId);

        // (3 * 50) + (1000 * 0.1) + (5 * 20) = 150 + 100 + 100 = 350
        assertEquals(350.0, result);
    }

    @Test
    void testPredictChurnRisk_HighRisk() {
        Long userId = 1L;
        LocalDate fifteenDaysAgo = LocalDate.now().minusDays(15);
        when(streakRepo.findByUserId(userId))
                .thenReturn(Optional.of(Streak.builder().lastActivityDate(fifteenDaysAgo).build()));

        double result = advancedAnalyticsService.predictChurnRisk(userId);

        assertEquals(0.5, result);
    }

    @Test
    void testPredictChurnRisk_NoStreakData() {
        Long userId = 1L;
        when(streakRepo.findByUserId(userId)).thenReturn(Optional.empty());

        double result = advancedAnalyticsService.predictChurnRisk(userId);

        // defaults to 30 days ago -> 30/30 = 1.0
        assertEquals(1.0, result);
    }

    @Test
    void testComputeInfluenceScore_NoData() {
        Long userId = 99L;
        when(userBadgeRepo.findByUserId(userId)).thenReturn(java.util.Collections.emptyList());
        when(growthRepo.findByUserId(userId)).thenReturn(Optional.empty());
        when(streakRepo.findByUserId(userId)).thenReturn(Optional.empty());

        double result = advancedAnalyticsService.computeInfluenceScore(userId);

        assertEquals(0.0, result);
    }

    @Test
    void testGetAiRecommendations() {
        Long userId = 1L;
        Map<String, Object> expected = Map.of("recommendation", "test");
        when(aiRecommendationService.getSmartRecommendations(userId)).thenReturn(expected);

        Map<String, Object> result = advancedAnalyticsService.getAiRecommendations(userId);

        assertEquals(expected, result);
    }

    @Test
    void testGetChurnPrediction() {
        Long userId = 1L;
        Map<String, Object> expected = Map.of("risk", "HIGH");
        when(mlPredictionService.predictChurnRisk(userId)).thenReturn(expected);

        Map<String, Object> result = advancedAnalyticsService.getChurnPrediction(userId);

        assertEquals(expected, result);
    }

    @Test
    void testGetModelStats() {
        Map<String, Object> expected = Map.of("acc", 0.9);
        when(mlPredictionService.getModelStats()).thenReturn(expected);

        Map<String, Object> result = advancedAnalyticsService.getModelStats();

        assertEquals(expected, result);
    }
}
