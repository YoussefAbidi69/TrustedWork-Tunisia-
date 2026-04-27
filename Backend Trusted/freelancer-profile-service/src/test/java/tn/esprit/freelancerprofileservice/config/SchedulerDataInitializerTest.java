package tn.esprit.freelancerprofileservice.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelancerprofileservice.entities.SchedulerConfig;
import tn.esprit.freelancerprofileservice.repositories.SchedulerConfigRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerDataInitializerTest {

    @Mock
    private SchedulerConfigRepository schedulerConfigRepository;

    @InjectMocks
    private SchedulerDataInitializer initializer;

    // ── Scénario 1 : aucune config en base → 4 insertions ────────────────────

    @Test
    void run_shouldInsertAll4Configs_whenNoneExist() {
        // Aucun job existant en base
        when(schedulerConfigRepository.findByJobName(any())).thenReturn(Optional.empty());

        initializer.run();

        // Doit sauvegarder exactement 4 configs
        verify(schedulerConfigRepository, times(4)).save(any(SchedulerConfig.class));
    }

    @Test
    void run_shouldInsertCorrectJobNames_whenNoneExist() {
        when(schedulerConfigRepository.findByJobName(any())).thenReturn(Optional.empty());

        ArgumentCaptor<SchedulerConfig> captor = ArgumentCaptor.forClass(SchedulerConfig.class);
        when(schedulerConfigRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        initializer.run();

        List<String> insertedJobs = captor.getAllValues().stream()
                .map(SchedulerConfig::getJobName)
                .toList();

        assertThat(insertedJobs).containsExactlyInAnyOrder(
                "recalculateAllSkillScores",
                "updateRegionalRankings",
                "sendProfileCompletionReminders",
                "checkCertificationExpiry"
        );
    }

    @Test
    void run_shouldInsertCorrectIntervals_whenNoneExist() {
        when(schedulerConfigRepository.findByJobName(any())).thenReturn(Optional.empty());

        ArgumentCaptor<SchedulerConfig> captor = ArgumentCaptor.forClass(SchedulerConfig.class);
        when(schedulerConfigRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        initializer.run();

        List<Integer> intervals = captor.getAllValues().stream()
                .map(SchedulerConfig::getIntervalMinutes)
                .toList();

        // recalculate=1440, rankings=10080, reminders=1440, certif=43200
        assertThat(intervals).containsExactlyInAnyOrder(1440, 10080, 1440, 43200);
    }

    @Test
    void run_shouldEnableAllJobs_byDefault() {
        when(schedulerConfigRepository.findByJobName(any())).thenReturn(Optional.empty());

        ArgumentCaptor<SchedulerConfig> captor = ArgumentCaptor.forClass(SchedulerConfig.class);
        when(schedulerConfigRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        initializer.run();

        captor.getAllValues().forEach(c ->
                assertThat(c.getEnabled()).isTrue()
        );
    }

    // ── Scénario 2 : toutes les configs déjà présentes → 0 insertion ─────────

    @Test
    void run_shouldInsertNothing_whenAllConfigsAlreadyExist() {
        // Simule que chaque job existe déjà en base
        SchedulerConfig existing = SchedulerConfig.builder()
                .jobName("recalculateAllSkillScores")
                .cronExpression("0 0 1 * * *")
                .intervalMinutes(1440)
                .enabled(true)
                .build();

        when(schedulerConfigRepository.findByJobName(any()))
                .thenReturn(Optional.of(existing));

        initializer.run();

        // Aucune insertion ne doit avoir lieu
        verify(schedulerConfigRepository, never()).save(any());
    }

    // ── Scénario 3 : une config manquante sur 4 → 1 seule insertion ──────────

    @Test
    void run_shouldInsertOnlyMissing_whenSomeConfigsExist() {
        SchedulerConfig existing = SchedulerConfig.builder()
                .jobName("recalculateAllSkillScores")
                .cronExpression("0 0 1 * * *")
                .intervalMinutes(1440)
                .enabled(true)
                .build();

        // 3 jobs déjà présents, 1 manquant
        when(schedulerConfigRepository.findByJobName("recalculateAllSkillScores"))
                .thenReturn(Optional.of(existing));
        when(schedulerConfigRepository.findByJobName("updateRegionalRankings"))
                .thenReturn(Optional.of(existing));
        when(schedulerConfigRepository.findByJobName("sendProfileCompletionReminders"))
                .thenReturn(Optional.of(existing));
        when(schedulerConfigRepository.findByJobName("checkCertificationExpiry"))
                .thenReturn(Optional.empty()); // manquant

        initializer.run();

        verify(schedulerConfigRepository, times(1)).save(any(SchedulerConfig.class));
    }
}
