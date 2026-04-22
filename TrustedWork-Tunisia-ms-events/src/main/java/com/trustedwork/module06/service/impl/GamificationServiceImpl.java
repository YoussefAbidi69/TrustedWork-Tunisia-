package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.dto.BadgeDTO;
import com.trustedwork.module06.dto.GrowthProfileDTO;
import com.trustedwork.module06.entity.*;
import com.trustedwork.module06.mapper.BadgeMapper;
import com.trustedwork.module06.mapper.GrowthProfileMapper;
import com.trustedwork.module06.repository.*;
import com.trustedwork.module06.service.GamificationService;
import com.trustedwork.module06.util.BadgeRules;
import com.trustedwork.module06.util.XPConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class GamificationServiceImpl implements GamificationService {

    private final GrowthProfileRepository growthRepo;
    private final BadgeRepository badgeRepo;
    private final UserBadgeRepository userBadgeRepo;
    private final StreakRepository streakRepo;
    private final EventRegistrationRepository registrationRepository;

    @Override
    public GrowthProfileDTO addXp(Long userId, int xpAmount, String reason) {
        GrowthProfile profile = growthRepo.findByUserId(userId)
                .orElseGet(() -> GrowthProfile.builder()
                        .userId(userId).xpPoints(0).level(1).engagementScore(0.0).build());

        profile.setXpPoints(profile.getXpPoints() + xpAmount);
        profile.setLevel(computeLevel(profile.getXpPoints()));
        
        // Intelligence: Calculate engagement score
        updateEngagementScore(profile);
        
        growthRepo.save(profile);

        updateStreak(userId);
        checkAndAwardBadges(userId, profile);

        GrowthProfileDTO dto = GrowthProfileMapper.toDto(profile);
        streakRepo.findByUserId(userId).ifPresent(s -> dto.setCurrentStreak(s.getCurrentStreak()));
        return dto;
    }

    private void updateEngagementScore(GrowthProfile profile) {
        // Nouvelle Logique : L'Engagement Score est la somme de l'XP et d'un gros bonus par Niveau
        // Ex: 3245 XP (Niveau 7) = 3245 + (7 * 250) = 4995 PTS d'engagement.
        double score = profile.getXpPoints() + (profile.getLevel() * 250.0);
        profile.setEngagementScore(score);
    }

    private int computeLevel(int xp) {
        return Math.max(1, xp / XPConstants.XP_PER_LEVEL + 1);
    }

    private void updateStreak(Long userId) {
        Streak streak = streakRepo.findByUserId(userId)
                .orElseGet(() -> Streak.builder()
                        .userId(userId).currentStreak(0).longestStreak(0).build());

        LocalDate today = LocalDate.now();
        if (streak.getLastActivityDate() == null ||
            streak.getLastActivityDate().isBefore(today.minusDays(1))) {
            streak.setCurrentStreak(1);
        } else if (streak.getLastActivityDate().isBefore(today)) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        }

        if (streak.getCurrentStreak() > streak.getLongestStreak())
            streak.setLongestStreak(streak.getCurrentStreak());

        streak.setLastActivityDate(today);
        streakRepo.save(streak);
    }

    private void checkAndAwardBadges(Long userId, GrowthProfile profile) {
        System.out.println("Checking badges for user " + userId + " (Level: " + profile.getLevel() + ")");
        
        // 1. Dynamic Level Badges (Automatic check for LEVEL_2, LEVEL_3, etc.)
        for (int i = 2; i <= profile.getLevel(); i++) {
            awardBadgeIfNew(userId, "LEVEL_" + i);
        }

        // 2. First Actions
        long regCount = registrationRepository.countByUserId(userId);
        if (regCount >= 1) awardBadgeIfNew(userId, BadgeRules.FIRST_EVENT);

        // 3. Activity Streaks
        streakRepo.findByUserId(userId).ifPresent(s -> {
            if (s.getCurrentStreak() >= 7)  awardBadgeIfNew(userId, BadgeRules.STREAK_7);
            if (s.getCurrentStreak() >= 30) awardBadgeIfNew(userId, BadgeRules.STREAK_30);
        });
    }

    @Override
    public void awardBadgeIfNew(Long userId, String badgeCode) {
        String cleanCode = badgeCode.trim();
        badgeRepo.findByCode(cleanCode).ifPresentOrElse(badge -> {
            boolean exists = userBadgeRepo.existsByUserIdAndBadgeId(userId, badge.getId());
            if (!exists) {
                System.out.println(">>> [GAMIFICATION] Awarding Badge: " + cleanCode + " to User: " + userId);
                UserBadge ub = UserBadge.builder().userId(userId).badge(badge).build();
                userBadgeRepo.saveAndFlush(ub);
                
                // Direct XP update without full re-check to avoid recursion
                growthRepo.findByUserId(userId).ifPresent(p -> {
                    p.setXpPoints(p.getXpPoints() + badge.getXpReward());
                    p.setLevel(computeLevel(p.getXpPoints()));
                    growthRepo.save(p);
                });
            } else {
                System.out.println(">>> [GAMIFICATION] User " + userId + " already has " + cleanCode);
            }
        }, () -> {
            System.out.println(">>> [GAMIFICATION] WARNING: Badge with code '" + cleanCode + "' not found in database.");
        });
    }

    @Override
    public List<BadgeDTO> getUserBadges(Long userId) {
        List<UserBadge> ubs = userBadgeRepo.findByUserId(userId);
        System.out.println(">>> [API] Found " + ubs.size() + " badges in DB for user " + userId);
        return ubs.stream()
                .map(ub -> BadgeMapper.toDto(ub.getBadge()))
                .collect(Collectors.toList());
    }

    @Override
    public GrowthProfileDTO getProfile(Long userId) {
        GrowthProfileDTO dto = GrowthProfileMapper.toDto(
                growthRepo.findByUserId(userId)
                        .orElseGet(() -> GrowthProfile.builder()
                                .userId(userId).xpPoints(0).level(1).build()));
        streakRepo.findByUserId(userId).ifPresent(s -> dto.setCurrentStreak(s.getCurrentStreak()));
        return dto;
    }

    @Override
    public List<GrowthProfileDTO> getAllProfiles() {
        return growthRepo.findAll().stream()
                .map(p -> {
                    GrowthProfileDTO dto = GrowthProfileMapper.toDto(p);
                    streakRepo.findByUserId(p.getUserId()).ifPresent(s -> dto.setCurrentStreak(s.getCurrentStreak()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeBadge(Long userId, Long badgeId) {
        System.out.println(">>> [GAMIFICATION] Attempting to remove badge " + badgeId + " from user " + userId);
        
        Badge badge = badgeRepo.findById(badgeId)
                .orElseThrow(() -> new RuntimeException("Badge not found"));

        // 1. Deduct XP from user profile
        growthRepo.findByUserId(userId).ifPresent(p -> {
            p.setXpPoints(Math.max(0, p.getXpPoints() - badge.getXpReward()));
            p.setLevel(computeLevel(p.getXpPoints()));
            updateEngagementScore(p);
            growthRepo.saveAndFlush(p);
            System.out.println(">>> [GAMIFICATION] Deducted " + badge.getXpReward() + " XP. New Total: " + p.getXpPoints());
        });

        // 2. Remove the badge assignment (finding the specific record first)
        List<UserBadge> ubs = userBadgeRepo.findByUserId(userId);
        ubs.stream()
           .filter(ub -> ub.getBadge().getId().equals(badgeId))
           .findFirst()
           .ifPresentOrElse(ub -> {
               userBadgeRepo.delete(ub);
               userBadgeRepo.flush();
               System.out.println(">>> [GAMIFICATION] Successfully deleted UserBadge record.");
           }, () -> {
               System.out.println(">>> [GAMIFICATION] WARNING: No UserBadge found for this user/badge combination.");
           });
    }
}
