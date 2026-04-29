package com.trustedwork.module06.dto;

import com.trustedwork.module06.enums.BadgeRarity;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class BadgeDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private BadgeRarity rarity;
    private int xpReward;
    private String iconUrl;
    private java.util.List<Long> ownerIds;
}
