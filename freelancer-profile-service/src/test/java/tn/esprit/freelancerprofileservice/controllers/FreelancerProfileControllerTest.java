package tn.esprit.freelancerprofileservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.enums.AvailabilityStatus;
import tn.esprit.freelancerprofileservice.security.JwtAuthFilter;
import tn.esprit.freelancerprofileservice.security.JwtUtil;
import tn.esprit.freelancerprofileservice.services.ICompletenessService;
import tn.esprit.freelancerprofileservice.services.IFreelancerProfileService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = FreelancerProfileController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class FreelancerProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IFreelancerProfileService profileService;

    @MockBean
    private ICompletenessService completenessService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private FreelancerProfile mockProfile() {
        FreelancerProfile p = new FreelancerProfile();
        p.setId(1L);
        p.setUserId(10L);
        p.setHeadline("Dev");
        p.setRegion("Tunis");
        p.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        return p;
    }

    @Test
    void shouldGetProfileByUserId() throws Exception {
        Mockito.when(profileService.getByUserId(10L)).thenReturn(mockProfile());

        mockMvc.perform(get("/api/profiles/user/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10));
    }

    @Test
    void shouldGetProfileById() throws Exception {
        Mockito.when(profileService.getById(1L)).thenReturn(mockProfile());

        mockMvc.perform(get("/api/profiles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldSearchProfiles() throws Exception {
        Mockito.when(profileService.searchProfiles(any(), any(), any(), any()))
                .thenReturn(List.of(mockProfile()));

        mockMvc.perform(get("/api/profiles/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldGetAllProfiles() throws Exception {
        Mockito.when(profileService.getAllPublicProfiles())
                .thenReturn(List.of(mockProfile()));

        mockMvc.perform(get("/api/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldDeleteProfile() throws Exception {
        mockMvc.perform(delete("/api/profiles/user/10"))
                .andExpect(status().isNoContent());
    }
}