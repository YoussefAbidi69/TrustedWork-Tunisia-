package com.trustedwork.module06.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustedwork.module06.dto.ChallengeDTO;
import com.trustedwork.module06.security.JwtUtil;
import com.trustedwork.module06.service.ChallengeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChallengeController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChallengeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChallengeService challengeService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getActiveChallenges_Success() throws Exception {
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);
        when(challengeService.getActiveChallenges(1L)).thenReturn(List.of(
                ChallengeDTO.builder().id(1L).title("Test Challenge").build()
        ));

        mockMvc.perform(get("/api/challenges")
                .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Challenge"));
    }

    @Test
    void joinChallenge_Success() throws Exception {
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);
        doNothing().when(challengeService).joinChallenge(1L, 100L);

        mockMvc.perform(post("/api/challenges/{id}/join", 100L)
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void succeedChallenge_Success() throws Exception {
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);
        doNothing().when(challengeService).succeedChallenge(1L, 100L);

        mockMvc.perform(post("/api/challenges/{id}/succeed", 100L)
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void claimReward_Success() throws Exception {
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);
        doNothing().when(challengeService).claimReward(1L, 100L);

        mockMvc.perform(post("/api/challenges/{id}/claim", 100L)
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllChallengesAdmin_Success() throws Exception {
        when(challengeService.getAllChallenges()).thenReturn(List.of(
                ChallengeDTO.builder().id(1L).title("Admin Challenge").build()
        ));

        mockMvc.perform(get("/api/challenges/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Admin Challenge"));
    }

    @Test
    void createChallengeAdmin_Success() throws Exception {
        ChallengeDTO dto = ChallengeDTO.builder().title("New Challenge").build();
        when(challengeService.createChallenge(any())).thenReturn(dto);

        mockMvc.perform(post("/api/challenges/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Challenge"));
    }
}
