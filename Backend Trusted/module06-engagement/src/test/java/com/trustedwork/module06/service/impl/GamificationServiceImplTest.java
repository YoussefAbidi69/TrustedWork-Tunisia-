package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.dto.GrowthProfileDTO;
import com.trustedwork.module06.entity.Badge;
import com.trustedwork.module06.entity.GrowthProfile;
import com.trustedwork.module06.entity.Streak;
import com.trustedwork.module06.entity.UserBadge;
import com.trustedwork.module06.repository.*;
import com.trustedwork.module06.util.BadgeRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

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

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Mocks par défaut pour éviter NPE
        lenient().when(registrationRepository.countByUserId(anyLong())).thenReturn(0L);
        lenient().when(userBadgeRepo.findByUserId(anyLong())).thenReturn(java.util.Collections.emptyList());
    }

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
    void testUpdateStreak_StreakBroken() {
        Long userId = 1L;
        Streak streak = Streak.builder()
                .userId(userId)
                .currentStreak(5)
                .lastActivityDate(LocalDate.now().minusDays(3))
                .build();
        
        when(streakRepo.findByUserId(userId)).thenReturn(Optional.of(streak));
        when(growthRepo.findByUserId(userId)).thenReturn(Optional.of(GrowthProfile.builder().userId(userId).build()));

        gamificationService.addXp(userId, 50, "Restart");

        assertEquals(1, streak.getCurrentStreak());
        verify(streakRepo, times(1)).save(streak);
    }

    @Test
    void testUpdateStreak_FirstActivity() {
        Long userId = 1L;
        when(streakRepo.findByUserId(userId)).thenReturn(Optional.empty());
        when(growthRepo.findByUserId(userId)).thenReturn(Optional.of(GrowthProfile.builder().userId(userId).build()));

        gamificationService.addXp(userId, 50, "First");

        verify(streakRepo, times(1)).save(argThat(s -> s.getCurrentStreak() == 1));
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

    @Test
    void testAwardBadgeIfNew_AlreadyOwned() {
        Long userId = 1L;
        String badgeCode = "LEVEL_2";
        Badge badge = Badge.builder().id(2L).code(badgeCode).build();

        when(badgeRepo.findByCode(badgeCode)).thenReturn(Optional.of(badge));
        when(userBadgeRepo.existsByUserIdAndBadgeId(userId, 2L)).thenReturn(true);

        gamificationService.awardBadgeIfNew(userId, badgeCode);

        verify(userBadgeRepo, never()).saveAndFlush(any());
    }

    @Test
    void testAwardBadgeIfNew_BadgeNotFound() {
        gamificationService.awardBadgeIfNew(1L, "UNKNOWN");
        verify(userBadgeRepo, never()).saveAndFlush(any());
    }

    @Test
    void testRemoveBadge_Success() {
        Long userId = 1L;
        Long badgeId = 2L;
        Badge badge = Badge.builder().id(badgeId).xpReward(500).build();
        GrowthProfile profile = GrowthProfile.builder().userId(userId).xpPoints(1000).build();
        UserBadge ub = UserBadge.builder().userId(userId).badge(badge).build();

        when(badgeRepo.findById(badgeId)).thenReturn(Optional.of(badge));
        when(growthRepo.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userBadgeRepo.findByUserId(userId)).thenReturn(List.of(ub));

        gamificationService.removeBadge(userId, badgeId);

        assertEquals(500, profile.getXpPoints());
        verify(userBadgeRepo, times(1)).delete(ub);
    }

    @Test
    void testRemoveBadge_NotFound() {
        when(badgeRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> gamificationService.removeBadge(1L, 99L));
    }

    @Test
    void testRemoveBadge_UserBadgeNotFound() {
        Long userId = 1L;
        Long badgeId = 2L;
        Badge badge = Badge.builder().id(badgeId).xpReward(500).build();
        when(badgeRepo.findById(badgeId)).thenReturn(Optional.of(badge));
        when(growthRepo.findByUserId(userId)).thenReturn(Optional.of(new GrowthProfile()));
        when(userBadgeRepo.findByUserId(userId)).thenReturn(List.of()); // Empty list

        gamificationService.removeBadge(userId, badgeId);
        // Should log warning but not throw exception
        verify(userBadgeRepo, never()).delete(any());
    }

    @Test
    void testAwardBadgeIfNew_ProfileNotFound() {
        Long userId = 1L;
        Badge badge = Badge.builder().id(2L).code("B").build();
        when(badgeRepo.findByCode("B")).thenReturn(Optional.of(badge));
        when(userBadgeRepo.existsByUserIdAndBadgeId(userId, 2L)).thenReturn(false);
        when(growthRepo.findByUserId(userId)).thenReturn(Optional.empty());

        gamificationService.awardBadgeIfNew(userId, "B");
        verify(userBadgeRepo, times(1)).saveAndFlush(any());
        verify(growthRepo, never()).save(any());
    }

    @Test
    void testGetAllProfiles() {
        when(growthRepo.findAll()).thenReturn(List.of(new GrowthProfile()));
        assertFalse(gamificationService.getAllProfiles().isEmpty());
    }

    @Test
    void testGetUserBadges() {
        Badge b = Badge.builder().id(1L).name("B").build();
        UserBadge ub = UserBadge.builder().badge(b).build();
        when(userBadgeRepo.findByUserId(1L)).thenReturn(List.of(ub));
        
        assertFalse(gamificationService.getUserBadges(1L).isEmpty());
    }

    @Test
    void testUpdateStreak_YesterdayActivity() {
        Long userId = 1L;
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Streak streak = Streak.builder().userId(userId).currentStreak(5).lastActivityDate(yesterday).build();
        
        when(streakRepo.findByUserId(userId)).thenReturn(Optional.of(streak));
        when(growthRepo.findByUserId(userId)).thenReturn(Optional.of(new GrowthProfile()));

        gamificationService.addXp(userId, 50, "Daily");

        assertEquals(6, streak.getCurrentStreak());
    }

    @Test
    void testUpdateStreak_TodayActivity_NoChange() {
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        Streak streak = Streak.builder().userId(userId).currentStreak(5).lastActivityDate(today).build();
        
        when(streakRepo.findByUserId(userId)).thenReturn(Optional.of(streak));
        when(growthRepo.findByUserId(userId)).thenReturn(Optional.of(new GrowthProfile()));

        gamificationService.addXp(userId, 50, "Double");

        assertEquals(5, streak.getCurrentStreak());
    }

    @Test
    void testAwardBadgeIfNew_Level1_NoLoop() {
        Long userId = 1L;
        GrowthProfile profile = GrowthProfile.builder().userId(userId).level(1).build();
        when(streakRepo.findByUserId(userId)).thenReturn(Optional.empty());

        // Reflection to call private checkAndAwardBadges
        ReflectionTestUtils.invokeMethod(gamificationService, "checkAndAwardBadges", userId, profile);
        
        verify(badgeRepo, never()).findByCode(startsWith("LEVEL_"));
    }

    @Test
    void testRemoveBadge_XpFloorZero() {
        Long userId = 1L;
        Badge badge = Badge.builder().id(1L).xpReward(1000).build();
        GrowthProfile profile = GrowthProfile.builder().userId(userId).xpPoints(100).build();
        when(badgeRepo.findById(1L)).thenReturn(Optional.of(badge));
        when(growthRepo.findByUserId(userId)).thenReturn(Optional.of(profile));

        gamificationService.removeBadge(userId, 1L);
        assertEquals(0, profile.getXpPoints());
    }
}
