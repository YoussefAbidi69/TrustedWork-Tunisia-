package tn.esprit.msprojectservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.msprojectservice.dto.DeliveryRiskSignalDTO;
import tn.esprit.msprojectservice.entities.*;
import tn.esprit.msprojectservice.repositories.IRiskSignalRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskSignalServiceImpl — Tests unitaires")
class RiskSignalServiceImplTest {

    @Mock
    private IRiskSignalRepository riskSignalRepository;

    @InjectMocks
    private RiskSignalServiceImpl riskSignalService;

    private Project sampleProject;
    private DeliveryRiskSignal activeSignal;
    private DeliveryRiskSignal resolvedSignal;

    @BeforeEach
    void setUp() {
        sampleProject = Project.builder()
                .id(1L)
                .title("Projet Alpha")
                .status(ProjectStatus.ACTIVE)
                .completionRate(30)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        activeSignal = DeliveryRiskSignal.builder()
                .id(10L)
                .project(sampleProject)
                .riskType(RiskType.DELAY_RISK)
                .severity(RiskSeverity.HIGH)
                .message("Risque de retard détecté — avancement insuffisant.")
                .resolved(false)
                .detectedAt(LocalDateTime.now().minusDays(2))
                .build();

        resolvedSignal = DeliveryRiskSignal.builder()
                .id(11L)
                .project(sampleProject)
                .riskType(RiskType.BOTTLENECK)
                .severity(RiskSeverity.MEDIUM)
                .message("Goulot d'étranglement détecté.")
                .resolved(true)
                .detectedAt(LocalDateTime.now().minusDays(5))
                .resolvedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getActiveRisksByProjectId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getActiveRisksByProjectId — retourne uniquement les signaux non résolus")
    void getActiveRisksByProjectId_shouldReturnUnresolvedSignals() {
        when(riskSignalRepository.findByProjectIdAndResolvedFalse(1L))
                .thenReturn(List.of(activeSignal));

        List<DeliveryRiskSignalDTO> results = riskSignalService.getActiveRisksByProjectId(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(10L);
        assertThat(results.get(0).isResolved()).isFalse();
    }

    @Test
    @DisplayName("getActiveRisksByProjectId — retourne liste vide si aucun signal actif")
    void getActiveRisksByProjectId_shouldReturnEmptyList_whenNone() {
        when(riskSignalRepository.findByProjectIdAndResolvedFalse(1L)).thenReturn(List.of());

        List<DeliveryRiskSignalDTO> results = riskSignalService.getActiveRisksByProjectId(1L);

        assertThat(results).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getLatestCriticalRisk
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getLatestCriticalRisk — retourne le signal le plus critique")
    void getLatestCriticalRisk_shouldReturnMostCritical() {
        when(riskSignalRepository.findLatestCriticalByProjectId(1L))
                .thenReturn(Optional.of(activeSignal));

        DeliveryRiskSignalDTO result = riskSignalService.getLatestCriticalRisk(1L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getSeverity()).isEqualTo(RiskSeverity.HIGH);
        assertThat(result.getRiskType()).isEqualTo(RiskType.DELAY_RISK);
    }

    @Test
    @DisplayName("getLatestCriticalRisk — lève RuntimeException si aucun signal actif")
    void getLatestCriticalRisk_shouldThrow_whenNoActiveSignals() {
        when(riskSignalRepository.findLatestCriticalByProjectId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskSignalService.getLatestCriticalRisk(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // resolveRisk
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("resolveRisk — marque le signal résolu et set resolvedAt")
    void resolveRisk_shouldMarkResolvedAndSetTimestamp() {
        when(riskSignalRepository.findById(10L)).thenReturn(Optional.of(activeSignal));
        when(riskSignalRepository.save(any(DeliveryRiskSignal.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeliveryRiskSignalDTO result = riskSignalService.resolveRisk(10L);

        assertThat(result.isResolved()).isTrue();
        assertThat(activeSignal.getResolvedAt()).isNotNull();
        verify(riskSignalRepository).save(activeSignal);
    }

    @Test
    @DisplayName("resolveRisk — lève RuntimeException si signal déjà résolu")
    void resolveRisk_shouldThrow_whenAlreadyResolved() {
        when(riskSignalRepository.findById(11L)).thenReturn(Optional.of(resolvedSignal));

        assertThatThrownBy(() -> riskSignalService.resolveRisk(11L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("déjà résolu");

        verify(riskSignalRepository, never()).save(any());
    }

    @Test
    @DisplayName("resolveRisk — lève RuntimeException si signal introuvable")
    void resolveRisk_shouldThrow_whenNotFound() {
        when(riskSignalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskSignalService.resolveRisk(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(riskSignalRepository, never()).save(any());
    }

    @Test
    @DisplayName("resolveRisk — resolvedAt est défini juste après résolution")
    void resolveRisk_shouldSetResolvedAtToCurrentTime() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        when(riskSignalRepository.findById(10L)).thenReturn(Optional.of(activeSignal));
        when(riskSignalRepository.save(any(DeliveryRiskSignal.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        riskSignalService.resolveRisk(10L);

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(activeSignal.getResolvedAt()).isBetween(before, after);
    }
}
