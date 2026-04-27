package com.trustedwork.module06.service;

import com.trustedwork.module06.dto.BadgeDTO;
import com.trustedwork.module06.dto.GrowthProfileDTO;
import java.util.List;

public interface GamificationService {
    GrowthProfileDTO addXp(Long userId, int xpAmount, String reason);
    void awardBadgeIfNew(Long userId, String badgeCode);
    List<BadgeDTO> getUserBadges(Long userId);
    GrowthProfileDTO getProfile(Long userId);
    List<GrowthProfileDTO> getAllProfiles();
    void removeBadge(Long userId, Long badgeId);
}
