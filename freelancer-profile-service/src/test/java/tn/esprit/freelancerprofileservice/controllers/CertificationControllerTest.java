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
import tn.esprit.freelancerprofileservice.dto.request.AddCertificationRequest;
import tn.esprit.freelancerprofileservice.dto.request.UpdateCertificationRequest;
import tn.esprit.freelancerprofileservice.entities.Certification;
import tn.esprit.freelancerprofileservice.enums.CertificationType;
import tn.esprit.freelancerprofileservice.security.JwtAuthFilter;
import tn.esprit.freelancerprofileservice.security.JwtUtil;
import tn.esprit.freelancerprofileservice.services.ICertificationService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CertificationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class CertificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ICertificationService certificationService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private Certification buildCertification() {
        return Certification.builder()
                .id(1L)
                .title("AWS Solutions Architect")
                .issuer("Amazon Web Services")
                .type(CertificationType.EXTERNAL)
                .issueDate(LocalDate.of(2023, 1, 1))
                .expiryDate(LocalDate.of(2026, 1, 1))
                .certificateUrl("https://example.com/cert")
                .isExpired(false)
                .build();
    }

    @Test
    void shouldAddCertification() throws Exception {
        AddCertificationRequest request = new AddCertificationRequest();
        request.setTitle("AWS Solutions Architect");
        request.setIssuer("Amazon Web Services");
        request.setType(CertificationType.EXTERNAL);
        request.setIssueDate(LocalDate.of(2023, 1, 1));
        request.setExpiryDate(LocalDate.of(2026, 1, 1));
        request.setCertificateUrl("https://example.com/cert");

        when(certificationService.addCertification(eq(1L), any(Certification.class)))
                .thenReturn(buildCertification());

        mockMvc.perform(post("/api/certifications/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("AWS Solutions Architect"))
                .andExpect(jsonPath("$.issuer").value("Amazon Web Services"));

        verify(certificationService).addCertification(eq(1L), any(Certification.class));
    }

    @Test
    void shouldUpdateCertification() throws Exception {
        UpdateCertificationRequest request = new UpdateCertificationRequest();
        request.setTitle("AWS DevOps Engineer");
        request.setIssuer("Amazon Web Services");

        Certification updated = buildCertification();
        updated.setTitle("AWS DevOps Engineer");

        when(certificationService.updateCertification(eq(1L), eq(1L), any(UpdateCertificationRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/certifications/1/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("AWS DevOps Engineer"));

        verify(certificationService).updateCertification(eq(1L), eq(1L), any(UpdateCertificationRequest.class));
    }

    @Test
    void shouldGetMyCertifications() throws Exception {
        when(certificationService.getMyCertifications(1L))
                .thenReturn(List.of(buildCertification()));

        mockMvc.perform(get("/api/certifications/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("AWS Solutions Architect"))
                .andExpect(jsonPath("$[0].issuer").value("Amazon Web Services"));

        verify(certificationService).getMyCertifications(1L);
    }

    @Test
    void shouldReturnEmptyListWhenNoCertifications() throws Exception {
        when(certificationService.getMyCertifications(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/certifications/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldDeleteCertification() throws Exception {
        mockMvc.perform(delete("/api/certifications/1/user/1"))
                .andExpect(status().isNoContent());

        verify(certificationService).deleteCertification(1L, 1L);
    }
}
