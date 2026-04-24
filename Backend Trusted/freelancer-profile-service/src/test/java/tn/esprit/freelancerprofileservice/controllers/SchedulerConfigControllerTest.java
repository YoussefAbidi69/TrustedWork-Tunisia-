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
import tn.esprit.freelancerprofileservice.dto.request.SchedulerConfigRequest;
import tn.esprit.freelancerprofileservice.entities.SchedulerConfig;
import tn.esprit.freelancerprofileservice.scheduler.ProfileScheduler;
import tn.esprit.freelancerprofileservice.security.JwtAuthFilter;
import tn.esprit.freelancerprofileservice.security.JwtUtil;
import tn.esprit.freelancerprofileservice.services.ISchedulerConfigService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = SchedulerConfigController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class SchedulerConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ISchedulerConfigService schedulerConfigService;

    @MockBean
    private ProfileScheduler profileScheduler;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private SchedulerConfig buildConfig(String jobName) {
        return SchedulerConfig.builder()
                .id(1L)
                .jobName(jobName)
                .description("Test description")
                .cronExpression("0 0 1 * * *")
                .intervalMinutes(1440)
                .enabled(true)
                .lastRun(null)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── GET /api/scheduler/config ────────────────────────────────────────────

    @Test
    void getAllConfigs_shouldReturn200WithList() throws Exception {
        SchedulerConfig config = buildConfig("recalculateAllSkillScores");
        when(schedulerConfigService.getAllConfigs()).thenReturn(List.of(config));

        mockMvc.perform(get("/api/scheduler/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobName").value("recalculateAllSkillScores"))
                .andExpect(jsonPath("$[0].intervalMinutes").value(1440))
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    void getAllConfigs_shouldReturn200WithEmptyList() throws Exception {
        when(schedulerConfigService.getAllConfigs()).thenReturn(List.of());

        mockMvc.perform(get("/api/scheduler/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/scheduler/config/{jobName} ──────────────────────────────────

    @Test
    void getConfigByJobName_shouldReturn200() throws Exception {
        SchedulerConfig config = buildConfig("checkCertificationExpiry");
        when(schedulerConfigService.getConfigByJobName("checkCertificationExpiry"))
                .thenReturn(config);

        mockMvc.perform(get("/api/scheduler/config/checkCertificationExpiry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobName").value("checkCertificationExpiry"));
    }

    // ── PUT /api/scheduler/config/{jobName} ──────────────────────────────────

    @Test
    void updateConfig_shouldReturn200WithUpdatedConfig() throws Exception {
        SchedulerConfigRequest request = new SchedulerConfigRequest();
        request.setCronExpression("0 0 3 * * *");
        request.setIntervalMinutes(720);
        request.setEnabled(false);

        SchedulerConfig updated = buildConfig("recalculateAllSkillScores");
        updated.setCronExpression("0 0 3 * * *");
        updated.setIntervalMinutes(720);
        updated.setEnabled(false);

        when(schedulerConfigService.updateConfig(eq("recalculateAllSkillScores"), any()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/scheduler/config/recalculateAllSkillScores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cronExpression").value("0 0 3 * * *"))
                .andExpect(jsonPath("$.intervalMinutes").value(720))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    // ── POST /api/scheduler/config/{jobName}/run ─────────────────────────────

    @Test
    void runJobNow_shouldReturn200AndTriggerJob() throws Exception {
        SchedulerConfig config = buildConfig("updateRegionalRankings");
        when(schedulerConfigService.getConfigByJobName("updateRegionalRankings"))
                .thenReturn(config);

        mockMvc.perform(post("/api/scheduler/config/updateRegionalRankings/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("triggered"))
                .andExpect(jsonPath("$.jobName").value("updateRegionalRankings"));
    }

    @Test
    void runJobNow_shouldReturn500_whenJobNotFound() throws Exception {
        when(schedulerConfigService.getConfigByJobName("unknownJob"))
                .thenThrow(new RuntimeException("Scheduler introuvable : unknownJob"));

        mockMvc.perform(post("/api/scheduler/config/unknownJob/run"))
                .andExpect(status().isInternalServerError());
    }
}
