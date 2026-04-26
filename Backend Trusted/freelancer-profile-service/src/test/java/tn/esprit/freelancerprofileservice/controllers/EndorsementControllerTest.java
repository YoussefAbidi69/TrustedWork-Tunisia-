package tn.esprit.freelancerprofileservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.freelancerprofileservice.dto.request.AddEndorsementRequest;
import tn.esprit.freelancerprofileservice.entities.Endorsement;
import tn.esprit.freelancerprofileservice.security.JwtAuthFilter;
import tn.esprit.freelancerprofileservice.security.JwtUtil;
import tn.esprit.freelancerprofileservice.services.IEndorsementService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = EndorsementController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class EndorsementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IEndorsementService endorsementService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private Endorsement buildEndorsement() {
        return Endorsement.builder()
                .id(1L)
                .endorserId(200L)
                .comment("Excellent développeur Java")
                .endorsedAt(LocalDateTime.of(2024, 5, 1, 10, 0))
                .build();
    }

    @Test
    void shouldAddEndorsement() throws Exception {
        AddEndorsementRequest request = new AddEndorsementRequest();
        request.setEndorserId(200L);
        request.setComment("Excellent développeur Java");

        when(endorsementService.addEndorsement(1L, 200L, "Excellent développeur Java"))
                .thenReturn(buildEndorsement());

        mockMvc.perform(post("/api/endorsements/skill/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.endorserId").value(200))
                .andExpect(jsonPath("$.comment").value("Excellent développeur Java"));

        verify(endorsementService).addEndorsement(1L, 200L, "Excellent développeur Java");
    }

    @Test
    void shouldGetEndorsementsBySkill() throws Exception {
        when(endorsementService.getEndorsementsBySkill(1L))
                .thenReturn(List.of(buildEndorsement()));

        mockMvc.perform(get("/api/endorsements/skill/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].endorserId").value(200))
                .andExpect(jsonPath("$[0].comment").value("Excellent développeur Java"));

        verify(endorsementService).getEndorsementsBySkill(1L);
    }

    @Test
    void shouldReturnEmptyList_whenNoEndorsements() throws Exception {
        when(endorsementService.getEndorsementsBySkill(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/endorsements/skill/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldCountEndorsementsBySkill() throws Exception {
        when(endorsementService.countEndorsements(1L)).thenReturn(5L);

        mockMvc.perform(get("/api/endorsements/skill/1/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));

        verify(endorsementService).countEndorsements(1L);
    }
}
