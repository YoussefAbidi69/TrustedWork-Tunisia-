package com.trustedwork.module06.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class GrowthProfileDTO {
    private Long id;
    private Long userId;
    private int xpPoints;
    private int xpToNextLevel;
    private int level;
    private double engagementScore;
    private int currentStreak;
}
