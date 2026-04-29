package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.dto.LeaderboardDTO;
import com.trustedwork.module06.entity.GrowthProfile;
import com.trustedwork.module06.entity.Leaderboard;
import com.trustedwork.module06.repository.GrowthProfileRepository;
import com.trustedwork.module06.repository.LeaderboardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceImplTest {

    @Mock
    private LeaderboardRepository leaderboardRepository;
    @Mock
    private GrowthProfileRepository growthProfileRepository;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private LeaderboardServiceImpl leaderboardService;

    @Test
    void testGetGlobalLeaderboard() {
        Leaderboard lb = Leaderboard.builder().userId(1L).engagementRank(1).build();
        when(leaderboardRepository.findAllByOrderByEngagementRankAsc()).thenReturn(List.of(lb));

        List<LeaderboardDTO> result = leaderboardService.getGlobalLeaderboard();

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getRank());
    }

    @Test
    void testRecomputeAllRanks() {
        GrowthProfile gp = GrowthProfile.builder().userId(1L).xpPoints(100).level(2).build();
        when(growthProfileRepository.findAll()).thenReturn(List.of(gp));
        when(leaderboardRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(leaderboardRepository.findAll()).thenReturn(new java.util.ArrayList<>(List.of(Leaderboard.builder().userId(1L).engagementScore(600.0).build())));

        leaderboardService.recomputeAllRanks();

        verify(leaderboardRepository, atLeastOnce()).save(any());
        verify(leaderboardRepository).saveAll(anyList());
    }

    @Test
    void testRecomputeAllRanks_ApiError() {
        GrowthProfile gp = GrowthProfile.builder().userId(1L).xpPoints(100).build();
        when(growthProfileRepository.findAll()).thenReturn(List.of(gp));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenThrow(new RuntimeException("API Down"));
        when(leaderboardRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(leaderboardRepository.findAll()).thenReturn(new java.util.ArrayList<>());

        assertDoesNotThrow(() -> leaderboardService.recomputeAllRanks());
    }
}
