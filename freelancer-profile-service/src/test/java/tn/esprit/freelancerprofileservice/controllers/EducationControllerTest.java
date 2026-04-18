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
import tn.esprit.freelancerprofileservice.dto.request.AddEducationRequest;
import tn.esprit.freelancerprofileservice.dto.request.UpdateEducationRequest;
import tn.esprit.freelancerprofileservice.entities.Education;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.security.JwtAuthFilter;
import tn.esprit.freelancerprofileservice.security.JwtUtil;
import tn.esprit.freelancerprofileservice.services.IEducationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = EducationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class EducationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IEducationService educationService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private Education buildEducation() {
        return Education.builder()
                .id(1L)
                .degree("Licence en Informatique")
                .institution("ESPRIT")
                .fieldOfStudy("Génie Logiciel")
                .graduationYear(2022)
                .build();
    }

    @Test
    void shouldAddEducation() throws Exception {
        AddEducationRequest request = new AddEducationRequest();
        request.setDegree("Licence en Informatique");
        request.setInstitution("ESPRIT");
        request.setFieldOfStudy("Génie Logiciel");
        request.setGraduationYear(2022);

        when(educationService.addEducation(eq(1L), any(Education.class)))
                .thenReturn(buildEducation());

        mockMvc.perform(post("/api/educations/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.degree").value("Licence en Informatique"))
                .andExpect(jsonPath("$.institution").value("ESPRIT"));

        verify(educationService).addEducation(eq(1L), any(Education.class));
    }

    @Test
    void shouldReturnBadRequest_whenAddThrowsRuntimeException() throws Exception {
        AddEducationRequest request = new AddEducationRequest();
        request.setDegree("Licence en Informatique");
        request.setInstitution("ESPRIT");
        request.setGraduationYear(2022);

        when(educationService.addEducation(eq(1L), any(Education.class)))
                .thenThrow(new DuplicateResourceException("Formation déjà existante"));

        mockMvc.perform(post("/api/educations/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetAllEducations() throws Exception {
        when(educationService.getMyEducations(1L)).thenReturn(List.of(buildEducation()));

        mockMvc.perform(get("/api/educations/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].degree").value("Licence en Informatique"))
                .andExpect(jsonPath("$[0].institution").value("ESPRIT"));

        verify(educationService).getMyEducations(1L);
    }

    @Test
    void shouldReturnEmptyList_whenNoEducations() throws Exception {
        when(educationService.getMyEducations(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/educations/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldUpdateEducation() throws Exception {
        UpdateEducationRequest request = new UpdateEducationRequest();
        request.setDegree("Master en Informatique");

        Education updated = buildEducation();
        updated.setDegree("Master en Informatique");

        when(educationService.updateEducation(eq(1L), eq(1L), any(UpdateEducationRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/educations/1/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.degree").value("Master en Informatique"));

        verify(educationService).updateEducation(eq(1L), eq(1L), any(UpdateEducationRequest.class));
    }

    @Test
    void shouldReturnBadRequest_whenUpdateThrowsRuntimeException() throws Exception {
        UpdateEducationRequest request = new UpdateEducationRequest();
        request.setDegree("Master en Informatique");

        when(educationService.updateEducation(eq(1L), eq(1L), any(UpdateEducationRequest.class)))
                .thenThrow(new DuplicateResourceException("Formation déjà existante"));

        mockMvc.perform(put("/api/educations/1/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteEducation() throws Exception {
        mockMvc.perform(delete("/api/educations/1/user/1"))
                .andExpect(status().isNoContent());

        verify(educationService).deleteEducation(1L, 1L);
    }
}
