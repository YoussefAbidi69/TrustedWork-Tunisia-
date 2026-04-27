package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.entity.*;
import com.trustedwork.module06.enums.RegistrationStatus;
import com.trustedwork.module06.repository.*;
import com.trustedwork.module06.service.EngagementScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EngagementScoreServiceImpl implements EngagementScoreService {

    private final EventRegistrationRepository regRepo;
    private final StreakRepository streakRepo;
    private final GrowthProfileRepository growthRepo;
    private final LeaderboardRepository leaderboardRepo;

    @Override
    public double computeEngagementScore(Long userId) {
        // event_participation × 0.30
        long attended = regRepo.findByUserIdAndStatus(userId, RegistrationStatus.ATTENDED).size();
        double eventScore = Math.min(attended / 10.0, 1.0) * 0.30;

        // streak_length × 0.25
        int streak = streakRepo.findByUserId(userId)
                .map(Streak::getCurrentStreak).orElse(0);
        double streakScore = Math.min(streak / 30.0, 1.0) * 0.25;

        // community_influence × 0.20 (basé sur XP total)
        int xp = growthRepo.findByUserId(userId)
                .map(GrowthProfile::getXpPoints).orElse(0);
        double influenceScore = Math.min(xp / 5000.0, 1.0) * 0.20;

        // challenge_completion × 0.25 (placeholder)
        double challengeScore = 0.25 * 0.25;

        double total = eventScore + streakScore + influenceScore + challengeScore;

        // Mise à jour leaderboard
        Leaderboard lb = leaderboardRepo.findByUserId(userId)
                .orElseGet(() -> Leaderboard.builder().userId(userId).build());
        lb.setEngagementScore(total);
        leaderboardRepo.save(lb);

        // Mise à jour growth profile
        growthRepo.findByUserId(userId).ifPresent(p -> {
            p.setEngagementScore(total);
            growthRepo.save(p);
        });

        return total;
    }
}
