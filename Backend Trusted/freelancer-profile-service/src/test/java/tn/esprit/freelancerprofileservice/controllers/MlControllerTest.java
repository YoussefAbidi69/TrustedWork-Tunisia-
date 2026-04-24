package tn.esprit.freelancerprofileservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.security.JwtAuthFilter;
import tn.esprit.freelancerprofileservice.security.JwtUtil;
import tn.esprit.freelancerprofileservice.services.MlServiceClient;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = MlController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class MlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MlServiceClient mlServiceClient;

    @MockBean
    private FreelancerProfileRepository profileRepository;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetTrustScore() throws Exception {
        FreelancerProfile profile = new FreelancerProfile();
        profile.setId(1L);
        profile.setUserId(10L);

        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        when(mlServiceClient.predictTrustScore(profile, true, false))
                .thenReturn(new MlServiceClient.TrustScoreResult("HIGH", 0.91));

        mockMvc.perform(get("/api/ml/profiles/user/10/trust-score")
                        .param("kycVerified", "true")
                        .param("twoFactorEnabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.level").value("HIGH"))
                .andExpect(jsonPath("$.confidence").value(0.91));
    }

    @Test
    void shouldAnalyzeSentiment() throws Exception {
        when(mlServiceClient.predictSentiment("great service"))
                .thenReturn(new MlServiceClient.SentimentResult("POSITIVE", 0.97));

        mockMvc.perform(post("/api/ml/reviews/analyze")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comment", "great service"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("great service"))
                .andExpect(jsonPath("$.sentiment").value("POSITIVE"))
                .andExpect(jsonPath("$.score").value(0.97));
    }

    @Test
    void shouldReturnBadRequestWhenCommentMissing() throws Exception {
        mockMvc.perform(post("/api/ml/reviews/analyze")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comment", ""))))
                .andExpect(status().isBadRequest());
    }
}