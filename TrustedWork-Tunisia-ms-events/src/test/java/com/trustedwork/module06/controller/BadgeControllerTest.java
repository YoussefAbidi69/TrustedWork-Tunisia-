package com.trustedwork.module06.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustedwork.module06.dto.BadgeDTO;
import com.trustedwork.module06.service.BadgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BadgeController.class)
@AutoConfigureMockMvc(addFilters = false)
class BadgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BadgeService badgeService;

    @MockBean
    private com.trustedwork.module06.security.JwtUtil jwtUtil;

    @MockBean
    private com.trustedwork.module06.security.JwtFilter jwtFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAll_Success() throws Exception {
        when(badgeService.getAllBadges()).thenReturn(List.of(
                BadgeDTO.builder().id(1L).name("Gold").build()
        ));

        mockMvc.perform(get("/api/badges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Gold"));
    }

    @Test
    void getById_Success() throws Exception {
        when(badgeService.getBadgeById(1L)).thenReturn(
                BadgeDTO.builder().id(1L).name("Gold").build()
        );

        mockMvc.perform(get("/api/badges/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gold"));
    }

    @Test
    void create_Success() throws Exception {
        BadgeDTO dto = BadgeDTO.builder().name("New").build();
        when(badgeService.createBadge(any())).thenReturn(dto);

        mockMvc.perform(post("/api/badges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"));
    }

    @Test
    void update_Success() throws Exception {
        BadgeDTO dto = BadgeDTO.builder().name("Updated").build();
        when(badgeService.updateBadge(anyLong(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/badges/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void delete_Success() throws Exception {
        doNothing().when(badgeService).deleteBadge(1L);
        mockMvc.perform(delete("/api/badges/{id}", 1L))
                .andExpect(status().isOk());
    }
}
