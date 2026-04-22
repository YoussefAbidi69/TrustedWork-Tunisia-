package com.trustedwork.module06.service;

import com.trustedwork.module06.dto.BadgeDTO;
import java.util.List;

public interface BadgeService {
    List<BadgeDTO> getAllBadges();
    BadgeDTO getBadgeById(Long id);
    BadgeDTO createBadge(BadgeDTO dto);
    BadgeDTO updateBadge(Long id, BadgeDTO dto);
    void deleteBadge(Long id);
}
