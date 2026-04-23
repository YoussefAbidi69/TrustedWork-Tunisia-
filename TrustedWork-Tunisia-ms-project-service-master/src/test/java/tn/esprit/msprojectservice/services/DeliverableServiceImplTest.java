package tn.esprit.msprojectservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.msprojectservice.dto.DeliverableDTO;
import tn.esprit.msprojectservice.entities.*;
import tn.esprit.msprojectservice.exceptions.EntityNotFoundException;
import tn.esprit.msprojectservice.repositories.IDeliverableRepository;
import tn.esprit.msprojectservice.repositories.IProjectRepository;
import tn.esprit.msprojectservice.repositories.ITaskRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliverableServiceImpl — Tests unitaires")
class DeliverableServiceImplTest {

    @Mock
    private IDeliverableRepository deliverableRepository;

    @Mock
    private IProjectRepository projectRepository;

    @Mock
    private ITaskRepository taskRepository;

    @Mock
    private IMailService mailService;

    @InjectMocks
    private DeliverableServiceImpl deliverableService;

    private Project sampleProject;
    private Task sampleTask;
    private Deliverable submittedDeliverable;

    @BeforeEach
    void setUp() {
        sampleProject = Project.builder()
                .id(1L)
                .title("Projet Alpha")
                .status(ProjectStatus.ACTIVE)
                .completionRate(50)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleTask = Task.builder()
                .id(5L)
                .title("Tâche liée")
                .status(TaskStatus.IN_REVIEW)
                .priority(TaskPriority.MEDIUM)
                .project(sampleProject)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        submittedDeliverable = Deliverable.builder()
                .id(20L)
                .title("Livrable final")
                .description("Documentation complète")
                .fileUrl("https://files.example.com/doc.pdf")
                .status(DeliverableStatus.SUBMITTED)
                .project(sampleProject)
                .task(sampleTask)
                .submittedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // submitDeliverable
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("submitDeliverable — sauvegarde le livrable sans tâche associée")
    void submitDeliverable_shouldSave_withoutTask() {
        DeliverableDTO dto = buildDeliverableDTO(null);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(deliverableRepository.save(any(Deliverable.class))).thenReturn(submittedDeliverable);

        DeliverableDTO result = deliverableService.submitDeliverable(1L, dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getStatus()).isEqualTo(DeliverableStatus.SUBMITTED);
        verify(deliverableRepository).save(any(Deliverable.class));
        verify(taskRepository, never()).findById(any());
    }

    @Test
    @DisplayName("submitDeliverable — associe la tâche quand taskId fourni")
    void submitDeliverable_shouldLinkTask_whenTaskIdProvided() {
        DeliverableDTO dto = buildDeliverableDTO(5L);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(taskRepository.findById(5L)).thenReturn(Optional.of(sampleTask));
        when(deliverableRepository.save(any(Deliverable.class))).thenReturn(submittedDeliverable);

        DeliverableDTO result = deliverableService.submitDeliverable(1L, dto);

        assertThat(result).isNotNull();
        verify(taskRepository).findById(5L);
        verify(deliverableRepository).save(any(Deliverable.class));
    }

    @Test
    @DisplayName("submitDeliverable — lève EntityNotFoundException si projet introuvable")
    void submitDeliverable_shouldThrow_whenProjectNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());
        DeliverableDTO dto = buildDeliverableDTO(null);

        assertThatThrownBy(() -> deliverableService.submitDeliverable(99L, dto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(deliverableRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitDeliverable — lève EntityNotFoundException si tâche introuvable")
    void submitDeliverable_shouldThrow_whenTaskNotFound() {
        DeliverableDTO dto = buildDeliverableDTO(99L);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliverableService.submitDeliverable(1L, dto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(deliverableRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getDeliverablesByProjectId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDeliverablesByProjectId — retourne les livrables du projet")
    void getDeliverablesByProjectId_shouldReturnList() {
        when(deliverableRepository.findByProjectId(1L)).thenReturn(List.of(submittedDeliverable));

        List<DeliverableDTO> results = deliverableService.getDeliverablesByProjectId(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Livrable final");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getDeliverableById
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDeliverableById — retourne le DTO si le livrable existe")
    void getDeliverableById_shouldReturnDTO_whenExists() {
        when(deliverableRepository.findById(20L)).thenReturn(Optional.of(submittedDeliverable));

        DeliverableDTO result = deliverableService.getDeliverableById(20L);

        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getTitle()).isEqualTo("Livrable final");
    }

    @Test
    @DisplayName("getDeliverableById — lève EntityNotFoundException si introuvable")
    void getDeliverableById_shouldThrow_whenNotFound() {
        when(deliverableRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliverableService.getDeliverableById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // reviewDeliverable
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("reviewDeliverable — approuve et déclenche l'envoi de mail")
    void reviewDeliverable_shouldApproveAndSendMail() {
        when(deliverableRepository.findById(20L)).thenReturn(Optional.of(submittedDeliverable));
        when(deliverableRepository.save(any(Deliverable.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeliverableDTO result = deliverableService.reviewDeliverable(
                20L, DeliverableStatus.APPROVED, "Excellent travail !");

        assertThat(result.getStatus()).isEqualTo(DeliverableStatus.APPROVED);
        assertThat(result.getReviewComment()).isEqualTo("Excellent travail !");
        assertThat(submittedDeliverable.getReviewedAt()).isNotNull();
        verify(mailService).envoyerNotificationReviewLivrable(any(Deliverable.class));
    }

    @Test
    @DisplayName("reviewDeliverable — rejette et déclenche l'envoi de mail")
    void reviewDeliverable_shouldRejectAndSendMail() {
        when(deliverableRepository.findById(20L)).thenReturn(Optional.of(submittedDeliverable));
        when(deliverableRepository.save(any(Deliverable.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeliverableDTO result = deliverableService.reviewDeliverable(
                20L, DeliverableStatus.REJECTED, "Non-conforme aux specs.");

        assertThat(result.getStatus()).isEqualTo(DeliverableStatus.REJECTED);
        verify(mailService).envoyerNotificationReviewLivrable(any(Deliverable.class));
    }

    @Test
    @DisplayName("reviewDeliverable — lève IllegalStateException si livrable déjà évalué")
    void reviewDeliverable_shouldThrow_whenAlreadyReviewed() {
        submittedDeliverable.setStatus(DeliverableStatus.APPROVED);

        when(deliverableRepository.findById(20L)).thenReturn(Optional.of(submittedDeliverable));

        assertThatThrownBy(() ->
                deliverableService.reviewDeliverable(20L, DeliverableStatus.REJECTED, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reviewe");

        verify(deliverableRepository, never()).save(any());
    }

    @Test
    @DisplayName("reviewDeliverable — lève IllegalArgumentException pour statut invalide (SUBMITTED)")
    void reviewDeliverable_shouldThrow_whenStatusIsSubmitted() {
        when(deliverableRepository.findById(20L)).thenReturn(Optional.of(submittedDeliverable));

        assertThatThrownBy(() ->
                deliverableService.reviewDeliverable(20L, DeliverableStatus.SUBMITTED, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("APPROVED ou REJECTED");
    }

    @Test
    @DisplayName("reviewDeliverable — le mail ne bloque pas la sauvegarde si il échoue")
    void reviewDeliverable_shouldNotFail_whenMailThrows() {
        when(deliverableRepository.findById(20L)).thenReturn(Optional.of(submittedDeliverable));
        when(deliverableRepository.save(any(Deliverable.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP error"))
                .when(mailService).envoyerNotificationReviewLivrable(any());

        assertThatCode(() ->
                deliverableService.reviewDeliverable(20L, DeliverableStatus.APPROVED, "OK"))
                .doesNotThrowAnyException();

        verify(deliverableRepository).save(any(Deliverable.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteDeliverable
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteDeliverable — supprime le livrable existant")
    void deleteDeliverable_shouldDelete_whenExists() {
        when(deliverableRepository.findById(20L)).thenReturn(Optional.of(submittedDeliverable));

        deliverableService.deleteDeliverable(20L);

        verify(deliverableRepository).delete(submittedDeliverable);
    }

    @Test
    @DisplayName("deleteDeliverable — lève EntityNotFoundException si livrable introuvable")
    void deleteDeliverable_shouldThrow_whenNotFound() {
        when(deliverableRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliverableService.deleteDeliverable(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(deliverableRepository, never()).delete(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private DeliverableDTO buildDeliverableDTO(Long taskId) {
        DeliverableDTO dto = new DeliverableDTO();
        dto.setTitle("Livrable final");
        dto.setDescription("Documentation complète");
        dto.setFileUrl("https://files.example.com/doc.pdf");
        dto.setTaskId(taskId);
        dto.setProjectId(1L);
        return dto;
    }
}
