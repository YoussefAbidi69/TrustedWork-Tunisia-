package tn.esprit.freelancerprofileservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelancerprofileservice.entities.SchedulerConfig;
import tn.esprit.freelancerprofileservice.repositories.SchedulerConfigRepository;
import tn.esprit.freelancerprofileservice.services.impl.SchedulerConfigServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerConfigServiceImplTest {

    @Mock
    private SchedulerConfigRepository schedulerConfigRepository;

    @InjectMocks
    private SchedulerConfigServiceImpl service;

    private SchedulerConfig config;

    @BeforeEach
    void setUp() {
        config = SchedulerConfig.builder()
                .id(1L)
                .jobName("recalculateAllSkillScores")
                .description("Recalcul nocturne des scores d'authenticité")
                .cronExpression("0 0 1 * * *")
                .intervalMinutes(1440)
                .enabled(true)
                .lastRun(null)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── getAllConfigs ─────────────────────────────────────────────────────────

    @Test
    void getAllConfigs_shouldReturnAllConfigs() {
        when(schedulerConfigRepository.findAll()).thenReturn(List.of(config));

        List<SchedulerConfig> result = service.getAllConfigs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobName()).isEqualTo("recalculateAllSkillScores");
        verify(schedulerConfigRepository).findAll();
    }

    @Test
    void getAllConfigs_shouldReturnEmptyList_whenNoneExist() {
        when(schedulerConfigRepository.findAll()).thenReturn(List.of());

        List<SchedulerConfig> result = service.getAllConfigs();

        assertThat(result).isEmpty();
    }

    // ── getConfigByJobName ────────────────────────────────────────────────────

    @Test
    void getConfigByJobName_shouldReturnConfig_whenExists() {
        when(schedulerConfigRepository.findByJobName("recalculateAllSkillScores"))
                .thenReturn(Optional.of(config));

        SchedulerConfig result = service.getConfigByJobName("recalculateAllSkillScores");

        assertThat(result).isNotNull();
        assertThat(result.getJobName()).isEqualTo("recalculateAllSkillScores");
        assertThat(result.getIntervalMinutes()).isEqualTo(1440);
    }

    @Test
    void getConfigByJobName_shouldThrow_whenNotFound() {
        when(schedulerConfigRepository.findByJobName("unknownJob"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getConfigByJobName("unknownJob"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("unknownJob");
    }

    // ── updateConfig ──────────────────────────────────────────────────────────

    @Test
    void updateConfig_shouldUpdateCronIntervalAndEnabled() {
        SchedulerConfig patch = new SchedulerConfig();
        patch.setCronExpression("0 0 2 * * *");
        patch.setIntervalMinutes(720);
        patch.setEnabled(false);

        when(schedulerConfigRepository.findByJobName("recalculateAllSkillScores"))
                .thenReturn(Optional.of(config));
        when(schedulerConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SchedulerConfig result = service.updateConfig("recalculateAllSkillScores", patch);

        assertThat(result.getCronExpression()).isEqualTo("0 0 2 * * *");
        assertThat(result.getIntervalMinutes()).isEqualTo(720);
        assertThat(result.getEnabled()).isFalse();
        verify(schedulerConfigRepository).save(config);
    }

    @Test
    void updateConfig_shouldThrow_whenJobNotFound() {
        when(schedulerConfigRepository.findByJobName("unknown"))
                .thenReturn(Optional.empty());

        SchedulerConfig patch = new SchedulerConfig();
        patch.setCronExpression("0 * * * * *");
        patch.setIntervalMinutes(1);
        patch.setEnabled(true);

        assertThatThrownBy(() -> service.updateConfig("unknown", patch))
                .isInstanceOf(RuntimeException.class);

        verify(schedulerConfigRepository, never()).save(any());
    }

    // ── markLastRun ───────────────────────────────────────────────────────────

    @Test
    void markLastRun_shouldUpdateLastRun_whenJobExists() {
        when(schedulerConfigRepository.findByJobName("recalculateAllSkillScores"))
                .thenReturn(Optional.of(config));
        when(schedulerConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.markLastRun("recalculateAllSkillScores");

        assertThat(config.getLastRun()).isNotNull();
        assertThat(config.getLastRun()).isBeforeOrEqualTo(LocalDateTime.now());
        verify(schedulerConfigRepository).save(config);
    }

    @Test
    void markLastRun_shouldDoNothing_whenJobNotFound() {
        when(schedulerConfigRepository.findByJobName("ghost"))
                .thenReturn(Optional.empty());

        // Ne doit pas lever d'exception
        assertThatCode(() -> service.markLastRun("ghost")).doesNotThrowAnyException();

        verify(schedulerConfigRepository, never()).save(any());
    }
}
