package com.trustedwork.module06.mapper;

import com.trustedwork.module06.dto.GrowthProfileDTO;
import com.trustedwork.module06.entity.GrowthProfile;

public class GrowthProfileMapper {

    private GrowthProfileMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static GrowthProfileDTO toDto(GrowthProfile profile) {
        if(profile == null) return null;
        return GrowthProfileDTO.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .xpPoints(profile.getXpPoints())
                .xpToNextLevel(500) // Based on XPConstants.XP_PER_LEVEL
                .level(profile.getLevel())
                .engagementScore(profile.getEngagementScore())
                .build();
    }
}
