package com.trustedwork.module06.controller;

import com.trustedwork.module06.dto.LeaderboardDTO;
import com.trustedwork.module06.service.LeaderboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaderboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaderboardService leaderboardService;

    @MockBean
    private com.trustedwork.module06.security.JwtUtil jwtUtil;

    @MockBean
    private com.trustedwork.module06.security.JwtFilter jwtFilter;

    @Test
    void getGlobal_Success() throws Exception {
        when(leaderboardService.getGlobalLeaderboard()).thenReturn(List.of(
                LeaderboardDTO.builder().userId(1L).governorate("Tunis").build()
        ));

        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].governorate").value("Tunis"));
    }

    @Test
    void getByGov_Success() throws Exception {
        when(leaderboardService.getLeaderboardByGovernorate("Tunis")).thenReturn(List.of());

        mockMvc.perform(get("/api/leaderboard/governorate/Tunis"))
                .andExpect(status().isOk());
    }

    @Test
    void recompute_Success() throws Exception {
        doNothing().when(leaderboardService).recomputeAllRanks();

        mockMvc.perform(post("/api/leaderboard/recompute"))
                .andExpect(status().isOk());
    }

    @Test
    void debug_Success() throws Exception {
        when(leaderboardService.getGlobalLeaderboard()).thenReturn(List.of());

        mockMvc.perform(get("/api/leaderboard/debug"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void debug_Error() throws Exception {
        when(leaderboardService.getGlobalLeaderboard()).thenThrow(new RuntimeException("Test Error"));

        mockMvc.perform(get("/api/leaderboard/debug"))
                .andExpect(status().isInternalServerError());
    }
}
