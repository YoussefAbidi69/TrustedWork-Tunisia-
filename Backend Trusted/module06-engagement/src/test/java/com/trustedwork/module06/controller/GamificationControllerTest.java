package com.trustedwork.module06.controller;

import com.trustedwork.module06.security.JwtUtil;
import com.trustedwork.module06.service.EngagementScoreService;
import com.trustedwork.module06.service.GamificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GamificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class GamificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GamificationService gamificationService;

    @MockBean
    private EngagementScoreService scoreService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void getMyProfile_Success() throws Exception {
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);
        when(gamificationService.getProfile(1L)).thenReturn(
                com.trustedwork.module06.dto.GrowthProfileDTO.builder().level(5).build()
        );

        mockMvc.perform(get("/api/gamification/profile")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(5));
    }

    @Test
    void getMyBadges_Success() throws Exception {
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);
        when(gamificationService.getUserBadges(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/gamification/badges")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void getScore_Success() throws Exception {
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);
        when(scoreService.computeEngagementScore(1L)).thenReturn(85.0);

        mockMvc.perform(get("/api/gamification/score")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.engagementScore").value(85.0));
    }

    @Test
    void getAllProfilesAdmin_Success() throws Exception {
        mockMvc.perform(get("/api/gamification/admin/profiles"))
                .andExpect(status().isOk());
    }

    @Test
    void getUserProfileAdmin_Success() throws Exception {
        when(gamificationService.getProfile(1L)).thenReturn(
                com.trustedwork.module06.dto.GrowthProfileDTO.builder().level(5).build()
        );
        mockMvc.perform(get("/api/gamification/admin/user/1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(5));
    }

    @Test
    void removeUserBadgeAdmin_Success() throws Exception {
        doNothing().when(gamificationService).removeBadge(1L, 2L);
        mockMvc.perform(delete("/api/gamification/admin/user/1/badges/2"))
                .andExpect(status().isOk());
    }
}
