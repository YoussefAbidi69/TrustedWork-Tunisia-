package tn.esprit.msprojectservice.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.msprojectservice.dto.ProgressReportDTO;
import tn.esprit.msprojectservice.entities.Project;
import tn.esprit.msprojectservice.repositories.IProjectRepository;
import tn.esprit.msprojectservice.services.IMailService;
import tn.esprit.msprojectservice.services.IProgressReportService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProgressReportRestController — Tests unitaires")
class ProgressReportRestControllerTest {

    @Mock private IProgressReportService progressReportService;
    @Mock private IMailService           mailService;
    @Mock private IProjectRepository     projectRepository;

    @InjectMocks
    private ProgressReportRestController controller;

    private ProgressReportDTO sampleReport;
    private Project sampleProject;

    @BeforeEach
    void setUp() {
        sampleReport = ProgressReportDTO.builder()
                .id(1L).projectId(1L)
                .completionRate(70).totalTasks(10).completedTasks(7)
                .activeRisks(1).openDeliverables(0)
                .summary("Bon avancement")
                .generatedAt(LocalDateTime.now())
                .build();

        sampleProject = Project.builder()
                .id(1L).title("Projet TrustedWork")
                .clientId(10L).freelancerId(20L)
                .build();
    }

    // ─── generateReport ───────────────────────────────────────────────────────

    @Test
    @DisplayName("generateReport — retourne HTTP 200 avec le rapport généré")
    void generateReport_validProject_returns200WithReport() {
        when(progressReportService.generateReport(1L)).thenReturn(sampleReport);

        ResponseEntity<ProgressReportDTO> response = controller.generateReport(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProjectId()).isEqualTo(1L);
        assertThat(response.getBody().getCompletionRate()).isEqualTo(70);
        verify(progressReportService).generateReport(1L);
    }

    @Test
    @DisplayName("generateReport — délègue au service de rapport")
    void generateReport_delegatesToProgressReportService() {
        when(progressReportService.generateReport(42L)).thenReturn(sampleReport);

        ResponseEntity<ProgressReportDTO> response = controller.generateReport(42L);

        assertThat(response.getBody()).isSameAs(sampleReport);
        verify(progressReportService, times(1)).generateReport(42L);
    }

    // ─── getReportHistory ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getReportHistory — retourne HTTP 200 avec la liste des rapports")
    void getReportHistory_validProject_returns200WithList() {
        ProgressReportDTO report2 = ProgressReportDTO.builder()
                .id(2L).projectId(1L).completionRate(50).build();

        when(progressReportService.getReportHistory(1L)).thenReturn(List.of(sampleReport, report2));

        ResponseEntity<List<ProgressReportDTO>> response = controller.getReportHistory(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(progressReportService).getReportHistory(1L);
    }

    @Test
    @DisplayName("getReportHistory — retourne liste vide si aucun rapport")
    void getReportHistory_noReports_returnsEmptyList() {
        when(progressReportService.getReportHistory(1L)).thenReturn(List.of());

        ResponseEntity<List<ProgressReportDTO>> response = controller.getReportHistory(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ─── sendTestEmail ────────────────────────────────────────────────────────

    @Test
    @DisplayName("sendTestEmail — génère rapport et envoie email → retourne 200 avec message")
    void sendTestEmail_validProject_generatesReportAndSendsEmail() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(progressReportService.generateReport(1L)).thenReturn(sampleReport);

        ResponseEntity<String> response = controller.sendTestEmail(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Projet TrustedWork");
        verify(mailService).envoyerRapportHebdomadaire(sampleProject, sampleReport);
    }

    @Test
    @DisplayName("sendTestEmail — projet introuvable → RuntimeException")
    void sendTestEmail_projectNotFound_throwsRuntimeException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.sendTestEmail(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("sendTestEmail — rapport généré avant l'envoi email")
    void sendTestEmail_orderOfOperations_reportGeneratedBeforeEmail() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(progressReportService.generateReport(1L)).thenReturn(sampleReport);

        controller.sendTestEmail(1L);

        var inOrder = inOrder(progressReportService, mailService);
        inOrder.verify(progressReportService).generateReport(1L);
        inOrder.verify(mailService).envoyerRapportHebdomadaire(sampleProject, sampleReport);
    }
}
