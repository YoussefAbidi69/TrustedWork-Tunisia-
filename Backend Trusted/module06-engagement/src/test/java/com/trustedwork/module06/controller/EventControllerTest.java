package com.trustedwork.module06.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustedwork.module06.dto.EventDTO;
import com.trustedwork.module06.entity.EventRegistration;
import com.trustedwork.module06.security.JwtUtil;
import com.trustedwork.module06.service.EventService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAll_Success() throws Exception {
        when(eventService.getAllEvents()).thenReturn(List.of(
                EventDTO.builder().id(1L).title("Test Event").build()
        ));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Event"));
    }

    @Test
    void getByGovernorate_Success() throws Exception {
        when(eventService.getEventsByGovernorate("Tunis")).thenReturn(List.of());
        mockMvc.perform(get("/api/events/governorate/Tunis"))
                .andExpect(status().isOk());
    }

    @Test
    void create_Success() throws Exception {
        EventDTO dto = EventDTO.builder().title("New").build();
        when(eventService.createEvent(any())).thenReturn(dto);

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New"));
    }

    @Test
    void register_Success() throws Exception {
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);
        when(eventService.registerToEvent(1L, 1L)).thenReturn(EventRegistration.builder().build());

        mockMvc.perform(post("/api/events/{eventId}/register", 1L)
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyRegisteredEvents_Success() throws Exception {
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);
        when(eventService.getMyRegisteredEventIds(1L)).thenReturn(List.of(1L, 2L));

        mockMvc.perform(get("/api/events/my-registrations")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(1));
    }

    @Test
    void update_Success() throws Exception {
        EventDTO dto = EventDTO.builder().title("Updated").build();
        when(eventService.updateEvent(anyLong(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/events/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void delete_Success() throws Exception {
        doNothing().when(eventService).deleteEvent(1L);
        mockMvc.perform(delete("/api/events/{id}", 1L))
                .andExpect(status().isOk());
    }
}
