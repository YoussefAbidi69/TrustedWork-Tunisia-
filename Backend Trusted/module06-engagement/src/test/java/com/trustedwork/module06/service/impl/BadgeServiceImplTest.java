package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.dto.BadgeDTO;
import com.trustedwork.module06.entity.Badge;
import com.trustedwork.module06.repository.BadgeRepository;
import com.trustedwork.module06.repository.UserBadgeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BadgeServiceImplTest {

    @Mock
    private BadgeRepository badgeRepository;
    @Mock
    private UserBadgeRepository userBadgeRepository;

    @InjectMocks
    private BadgeServiceImpl badgeService;

    @Test
    void testGetAllBadges() {
        Badge badge = Badge.builder().id(1L).name("Test Badge").build();
        when(badgeRepository.findAll()).thenReturn(List.of(badge));
        when(userBadgeRepository.findByBadgeId(1L)).thenReturn(Collections.emptyList());

        List<BadgeDTO> result = badgeService.getAllBadges();

        assertEquals(1, result.size());
        assertEquals("Test Badge", result.get(0).getName());
    }

    @Test
    void testGetBadgeById() {
        Long id = 1L;
        Badge badge = Badge.builder().id(id).name("Test").build();
        when(badgeRepository.findById(id)).thenReturn(Optional.of(badge));

        BadgeDTO result = badgeService.getBadgeById(id);

        assertNotNull(result);
        assertEquals("Test", result.getName());
    }
}
