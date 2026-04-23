package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.dto.BadgeDTO;
import com.trustedwork.module06.entity.Badge;
import com.trustedwork.module06.exception.ResourceNotFoundException;
import com.trustedwork.module06.mapper.BadgeMapper;
import com.trustedwork.module06.repository.BadgeRepository;
import com.trustedwork.module06.repository.UserBadgeRepository;
import com.trustedwork.module06.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    @Override
    public List<BadgeDTO> getAllBadges() {
        return badgeRepository.findAll().stream()
                .map(badge -> {
                    var ownerships = userBadgeRepository.findByBadgeId(badge.getId());
                    return BadgeMapper.toDto(badge, ownerships);
                })
                .toList();
    }

    @Override
    public BadgeDTO getBadgeById(Long id) {
        Badge badge = badgeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Badge not found: " + id));
        var ownerships = userBadgeRepository.findByBadgeId(badge.getId());
        return BadgeMapper.toDto(badge, ownerships);
    }

    @Override
    public BadgeDTO createBadge(BadgeDTO dto) {
        Badge badge = BadgeMapper.toEntity(dto);
        return BadgeMapper.toDto(badgeRepository.save(badge));
    }

    @Override
    public BadgeDTO updateBadge(Long id, BadgeDTO dto) {
        Badge badge = badgeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Badge not found: " + id));
        
        badge.setName(dto.getName());
        badge.setCode(dto.getCode());
        badge.setDescription(dto.getDescription());
        badge.setRarity(dto.getRarity());
        badge.setXpReward(dto.getXpReward());
        badge.setIconUrl(dto.getIconUrl());
        
        return BadgeMapper.toDto(badgeRepository.save(badge));
    }

    @Override
    public void deleteBadge(Long id) {
        if (!badgeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Badge not found: " + id);
        }
        badgeRepository.deleteById(id);
    }
}
