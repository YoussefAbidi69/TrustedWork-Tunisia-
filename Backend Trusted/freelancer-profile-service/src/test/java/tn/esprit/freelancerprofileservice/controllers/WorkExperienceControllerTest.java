package tn.esprit.freelancerprofileservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.freelancerprofileservice.dto.request.AddWorkExperienceRequest;
import tn.esprit.freelancerprofileservice.entities.WorkExperience;
import tn.esprit.freelancerprofileservice.security.JwtAuthFilter;
import tn.esprit.freelancerprofileservice.security.JwtUtil;
import tn.esprit.freelancerprofileservice.services.IWorkExperienceService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = WorkExperienceController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class WorkExperienceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IWorkExperienceService workExperienceService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private WorkExperience buildExperience() {
        WorkExperience w = new WorkExperience();
        w.setId(1L);
        w.setJobTitle("Software Engineer");
        w.setCompany("Esprit");
        w.setLocation("Tunis");
        w.setDescription("Backend");
        w.setStartDate(LocalDate.of(2022, 1, 1));
        w.setEndDate(LocalDate.of(2023, 1, 1));
        w.setIsCurrent(false);
        return w;
    }

    @Test
    void shouldAddWorkExperience() throws Exception {
        AddWorkExperienceRequest request = new AddWorkExperienceRequest();
        request.setJobTitle("Software Engineer");
        request.setCompany("Esprit");
        request.setLocation("Tunis");
        request.setDescription("Backend");
        request.setStartDate(LocalDate.of(2022, 1, 1));
        request.setEndDate(LocalDate.of(2023, 1, 1));
        request.setIsCurrent(false);

        when(workExperienceService.addWorkExperience(eq(1L), any(WorkExperience.class)))
                .thenReturn(buildExperience());

        mockMvc.perform(post("/api/work-experiences/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobTitle").value("Software Engineer"))
                .andExpect(jsonPath("$.company").value("Esprit"));

        verify(workExperienceService).addWorkExperience(eq(1L), any(WorkExperience.class));
    }

    @Test
    void shouldUpdateWorkExperience() throws Exception {
        AddWorkExperienceRequest request = new AddWorkExperienceRequest();
        request.setJobTitle("Senior Engineer");
        request.setCompany("Esprit");
        request.setLocation("Tunis");
        request.setDescription("Backend");
        request.setStartDate(LocalDate.of(2022, 1, 1));
        request.setEndDate(LocalDate.of(2023, 1, 1));
        request.setIsCurrent(false);

        WorkExperience updated = buildExperience();
        updated.setJobTitle("Senior Engineer");

        when(workExperienceService.updateWorkExperience(eq(1L), eq(1L), any(WorkExperience.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/work-experiences/1/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("Senior Engineer"));

        verify(workExperienceService).updateWorkExperience(eq(1L), eq(1L), any(WorkExperience.class));
    }

    @Test
    void shouldGetAllWorkExperiences() throws Exception {
        when(workExperienceService.getMyWorkExperiences(1L))
                .thenReturn(List.of(buildExperience()));

        mockMvc.perform(get("/api/work-experiences/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(workExperienceService).getMyWorkExperiences(1L);
    }

    @Test
    void shouldGetWorkExperienceById() throws Exception {
        when(workExperienceService.getWorkExperienceById(1L, 1L))
                .thenReturn(buildExperience());

        mockMvc.perform(get("/api/work-experiences/1/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").value("Esprit"));

        verify(workExperienceService).getWorkExperienceById(1L, 1L);
    }

    @Test
    void shouldDeleteWorkExperience() throws Exception {
        mockMvc.perform(delete("/api/work-experiences/1/user/1"))
                .andExpect(status().isNoContent());

        verify(workExperienceService).deleteWorkExperience(1L, 1L);
    }

    @Test
    void shouldGetTotalDuration() throws Exception {
        when(workExperienceService.getTotalExperienceInMonths(1L)).thenReturn(24L);

        mockMvc.perform(get("/api/work-experiences/user/1/total-duration"))
                .andExpect(status().isOk())
                .andExpect(content().string("24"));

        verify(workExperienceService).getTotalExperienceInMonths(1L);
    }
}