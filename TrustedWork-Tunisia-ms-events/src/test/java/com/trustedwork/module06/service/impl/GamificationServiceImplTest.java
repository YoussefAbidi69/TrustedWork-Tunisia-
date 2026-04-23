package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.dto.GrowthProfileDTO;
import com.trustedwork.module06.entity.Badge;
import com.trustedwork.module06.entity.GrowthProfile;
import com.trustedwork.module06.entity.Streak;
import com.trustedwork.module06.repository.*;
import com.trustedwork.module06.util.BadgeRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationServiceImplTest {

    @Mock
    private GrowthProfileRepository growthRepo;
    @Mock
    private BadgeRepository badgeRepo;
    @Mock
    private UserBadgeRepository userBadgeRepo;
    @Mock
    private StreakRepository streakRepo;
    @Mock
    private EventRegistrationRepository registrationRepository;

    @InjectMocks
    private GamificationServiceImpl gamificationService;

    @Test
    void testAddXp_NewUser() {
        Long userId = 1L;
        when(growthRepo.findByUserId(userId)).thenReturn(Optional.empty());
        when(streakRepo.findByUserId(userId)).thenReturn(Optional.empty());

        GrowthProfileDTO result = gamificationService.addXp(userId, 100, "Test");

        assertNotNull(result);
        assertEquals(100, result.getXpPoints());
        assertEquals(1, result.getLevel());
        verify(growthRepo, times(1)).save(any());
    }

    @Test
    void testUpdateStreak_DailyActivity() {
        Long userId = 1L;
        Streak streak = Streak.builder()
                .userId(userId)
                .currentStreak(5)
                .lastActivityDate(LocalDate.now().minusDays(1))
                .build();
        
        when(streakRepo.findByUserId(userId)).thenReturn(Optional.of(streak));
        when(growthRepo.findByUserId(userId)).thenReturn(Optional.of(GrowthProfile.builder().userId(userId).build()));

        gamificationService.addXp(userId, 50, "Daily");

        assertEquals(6, streak.getCurrentStreak());
        verify(streakRepo, times(1)).save(streak);
    }

    @Test
    void testAwardBadgeIfNew_Success() {
        Long userId = 1L;
        String badgeCode = BadgeRules.FIRST_EVENT;
        Badge badge = Badge.builder().id(10L).code(badgeCode).xpReward(100).build();

        when(badgeRepo.findByCode(badgeCode)).thenReturn(Optional.of(badge));
        when(userBadgeRepo.existsByUserIdAndBadgeId(userId, 10L)).thenReturn(false);
        when(growthRepo.findByUserId(userId)).thenReturn(Optional.of(GrowthProfile.builder().userId(userId).xpPoints(0).build()));

        gamificationService.awardBadgeIfNew(userId, badgeCode);

        verify(userBadgeRepo, times(1)).saveAndFlush(any());
        verify(growthRepo, times(1)).save(any());
    }
}
