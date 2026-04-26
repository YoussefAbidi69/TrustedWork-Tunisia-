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
import tn.esprit.freelancerprofileservice.dto.request.AddReportRequest;
import tn.esprit.freelancerprofileservice.dto.response.ProfileReportResponse;
import tn.esprit.freelancerprofileservice.entities.ProfileReport;
import tn.esprit.freelancerprofileservice.enums.ReportCategory;
import tn.esprit.freelancerprofileservice.enums.ReportStatus;
import tn.esprit.freelancerprofileservice.security.JwtAuthFilter;
import tn.esprit.freelancerprofileservice.security.JwtUtil;
import tn.esprit.freelancerprofileservice.services.IProfileReportService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ReportController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IProfileReportService reportService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateReport() throws Exception {
        AddReportRequest request = new AddReportRequest();
        request.setReporterId(2L);
        request.setCategory(ReportCategory.SPAM);
        request.setDescription("fake profile");

        ProfileReport report = new ProfileReport();
        report.setId(1L);
        report.setReporterId(2L);
        report.setCategory(ReportCategory.SPAM);
        report.setDescription("fake profile");
        report.setStatus(ReportStatus.PENDING);

        when(reportService.reportProfile(1L, 2L, ReportCategory.SPAM, "fake profile"))
                .thenReturn(report);

        mockMvc.perform(post("/api/reports/profile/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldGetAllReports() throws Exception {
        ProfileReportResponse response = ProfileReportResponse.builder()
                .id(1L)
                .profileId(1L)
                .reporterId(2L)
                .status(ReportStatus.PENDING)
                .build();

        when(reportService.getAllReports()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldGetPendingReports() throws Exception {
        when(reportService.getPendingReports()).thenReturn(List.of(
                ProfileReportResponse.builder().id(1L).status(ReportStatus.PENDING).build()
        ));

        mockMvc.perform(get("/api/reports/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldGetReportsByStatus() throws Exception {
        when(reportService.getReportsByStatus(ReportStatus.RESOLVED)).thenReturn(List.of(
                ProfileReportResponse.builder().id(1L).status(ReportStatus.RESOLVED).build()
        ));

        mockMvc.perform(get("/api/reports/status/RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RESOLVED"));
    }

    @Test
    void shouldGetReportsByProfileId() throws Exception {
        when(reportService.getReportsByProfileId(1L)).thenReturn(List.of(
                ProfileReportResponse.builder().id(1L).profileId(1L).build()
        ));

        mockMvc.perform(get("/api/reports/profile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].profileId").value(1));
    }

    @Test
    void shouldUpdateReportStatus() throws Exception {
        when(reportService.updateReportStatus(1L, ReportStatus.REJECTED))
                .thenReturn(ProfileReportResponse.builder()
                        .id(1L)
                        .status(ReportStatus.REJECTED)
                        .build());

        mockMvc.perform(patch("/api/reports/1/status")
                        .param("status", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}