package com.trustedwork.module06.mapper;

import com.trustedwork.module06.dto.BadgeDTO;
import com.trustedwork.module06.entity.Badge;
import com.trustedwork.module06.entity.UserBadge;
import java.util.List;
import java.util.stream.Collectors;

public class BadgeMapper {

    private BadgeMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static BadgeDTO toDto(Badge badge) {
        return toDto(badge, null);
    }

    public static BadgeDTO toDto(Badge badge, List<UserBadge> userBadges) {
        if(badge == null) return null;
        
        List<Long> ownerIds = (userBadges != null) 
            ? userBadges.stream().map(UserBadge::getUserId).collect(Collectors.toList())
            : null;

        return BadgeDTO.builder()
                .id(badge.getId())
                .code(badge.getCode())
                .name(badge.getName())
                .description(badge.getDescription())
                .rarity(badge.getRarity())
                .xpReward(badge.getXpReward())
                .iconUrl(badge.getIconUrl())
                .ownerIds(ownerIds)
                .build();
    }
    public static Badge toEntity(BadgeDTO dto) {
        if(dto == null) return null;
        return Badge.builder()
                .id(dto.getId())
                .code(dto.getCode())
                .name(dto.getName())
                .description(dto.getDescription())
                .rarity(dto.getRarity())
                .xpReward(dto.getXpReward())
                .iconUrl(dto.getIconUrl())
                .build();
    }
}
