package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.entity.GrowthProfile;
import com.trustedwork.module06.entity.Leaderboard;
import com.trustedwork.module06.entity.Streak;
import com.trustedwork.module06.enums.RegistrationStatus;
import com.trustedwork.module06.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EngagementScoreServiceImplTest {

    @Mock
    private EventRegistrationRepository regRepo;
    @Mock
    private StreakRepository streakRepo;
    @Mock
    private GrowthProfileRepository growthRepo;
    @Mock
    private LeaderboardRepository leaderboardRepo;

    @InjectMocks
    private EngagementScoreServiceImpl engagementScoreService;

    @Test
    void testComputeEngagementScore_UserExists() {
        Long userId = 1L;

        // Mock event participation (attended 5 events)
        when(regRepo.findByUserIdAndStatus(userId, RegistrationStatus.ATTENDED))
                .thenReturn(Collections.nCopies(5, null)); // Size 5

        // Mock streak (20 days)
        when(streakRepo.findByUserId(userId))
                .thenReturn(Optional.of(Streak.builder().currentStreak(20).build()));

        // Mock growth profile (2500 XP)
        when(growthRepo.findByUserId(userId))
                .thenReturn(Optional.of(GrowthProfile.builder().xpPoints(2500).build()));

        // Mock leaderboard
        when(leaderboardRepo.findByUserId(userId))
                .thenReturn(Optional.of(Leaderboard.builder().userId(userId).build()));

        // Execute
        double score = engagementScoreService.computeEngagementScore(userId);

        // Verify
        assertTrue(score > 0);
        verify(leaderboardRepo, times(1)).save(any());
    }

    @Test
    void testComputeEngagementScore_UserNotFound() {
        Long userId = 99L;

        when(regRepo.findByUserIdAndStatus(userId, RegistrationStatus.ATTENDED))
                .thenReturn(Collections.emptyList());
        when(streakRepo.findByUserId(userId)).thenReturn(Optional.empty());
        when(growthRepo.findByUserId(userId)).thenReturn(Optional.empty());
        when(leaderboardRepo.findByUserId(userId)).thenReturn(Optional.empty());

        double score = engagementScoreService.computeEngagementScore(userId);

        assertTrue(score > 0); // Still has challengeScore placeholder
        verify(leaderboardRepo, times(1)).save(any());
    }
}
