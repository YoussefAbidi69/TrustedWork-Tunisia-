package tn.esprit.msprojectservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.msprojectservice.dto.TaskDTO;
import tn.esprit.msprojectservice.entities.*;
import tn.esprit.msprojectservice.repositories.IProjectRepository;
import tn.esprit.msprojectservice.repositories.ITaskRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskServiceImpl — Tests unitaires")
class TaskServiceImplTest {

    @Mock
    private ITaskRepository taskRepository;

    @Mock
    private IProjectRepository projectRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Project sampleProject;
    private Task sampleTask;

    @BeforeEach
    void setUp() {
        sampleProject = Project.builder()
                .id(1L)
                .title("Projet Alpha")
                .status(ProjectStatus.ACTIVE)
                .completionRate(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleTask = Task.builder()
                .id(10L)
                .title("Tâche principale")
                .description("Description de la tâche")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .project(sampleProject)
                .assigneeId(200L)
                .deadline(LocalDate.of(2024, 6, 30))
                .estimatedHours(8)
                .actualHours(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createTask
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createTask — crée la tâche et recalcule le completionRate")
    void createTask_shouldCreateAndUpdateCompletionRate() {
        TaskDTO dto = buildTaskDTO();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);
        when(taskRepository.countTotalTasksByProjectId(1L)).thenReturn(1);
        when(taskRepository.countCompletedTasksByProjectId(1L)).thenReturn(0);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);

        TaskDTO result = taskService.createTask(1L, dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        verify(taskRepository).save(any(Task.class));
        verify(projectRepository, atLeastOnce()).save(any(Project.class));
    }

    @Test
    @DisplayName("createTask — lève RuntimeException si projet introuvable")
    void createTask_shouldThrow_whenProjectNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(99L, buildTaskDTO()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(taskRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getTaskById
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTaskById — retourne le DTO si la tâche existe")
    void getTaskById_shouldReturnDTO_whenExists() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));

        TaskDTO result = taskService.getTaskById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTitle()).isEqualTo("Tâche principale");
    }

    @Test
    @DisplayName("getTaskById — lève RuntimeException si tâche introuvable")
    void getTaskById_shouldThrow_whenNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getTasksByProjectId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTasksByProjectId — retourne la liste des tâches du projet")
    void getTasksByProjectId_shouldReturnList() {
        Task t2 = Task.builder().id(11L).title("Tâche B").status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.MEDIUM).project(sampleProject)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(sampleTask, t2));

        List<TaskDTO> results = taskService.getTasksByProjectId(1L);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(TaskDTO::getId).containsExactly(10L, 11L);
    }

    @Test
    @DisplayName("getTasksByProjectId — retourne liste vide si aucune tâche")
    void getTasksByProjectId_shouldReturnEmptyList_whenNoTasks() {
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of());

        assertThat(taskService.getTasksByProjectId(1L)).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateTask
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateTask — met à jour les champs modifiables")
    void updateTask_shouldUpdateFields() {
        TaskDTO updateDTO = buildTaskDTO();
        updateDTO.setTitle("Titre modifié");
        updateDTO.setEstimatedHours(16);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskDTO result = taskService.updateTask(10L, updateDTO);

        assertThat(result.getTitle()).isEqualTo("Titre modifié");
        assertThat(result.getEstimatedHours()).isEqualTo(16);
        verify(taskRepository).save(sampleTask);
    }

    @Test
    @DisplayName("updateTask — lève RuntimeException si tâche introuvable")
    void updateTask_shouldThrow_whenNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask(99L, buildTaskDTO()))
                .isInstanceOf(RuntimeException.class);

        verify(taskRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateTaskStatus
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateTaskStatus — change le statut et recalcule le completionRate")
    void updateTaskStatus_shouldChangeStatusAndRecalculate() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.countTotalTasksByProjectId(1L)).thenReturn(3);
        when(taskRepository.countCompletedTasksByProjectId(1L)).thenReturn(1);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);

        TaskDTO result = taskService.updateTaskStatus(10L, TaskStatus.DONE);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("updateTaskStatus — completionRate = 100 quand toutes les tâches sont DONE")
    void updateTaskStatus_shouldSetHundredPercent_whenAllDone() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.countTotalTasksByProjectId(1L)).thenReturn(2);
        when(taskRepository.countCompletedTasksByProjectId(1L)).thenReturn(2);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project saved = inv.getArgument(0);
            assertThat(saved.getCompletionRate()).isEqualTo(100);
            return saved;
        });

        taskService.updateTaskStatus(10L, TaskStatus.DONE);

        verify(projectRepository).save(any(Project.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // assignTask
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("assignTask — affecte la tâche à un utilisateur")
    void assignTask_shouldSetAssigneeId() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskDTO result = taskService.assignTask(10L, 300L);

        assertThat(result.getAssigneeId()).isEqualTo(300L);
        verify(taskRepository).save(sampleTask);
    }

    @Test
    @DisplayName("assignTask — lève RuntimeException si tâche introuvable")
    void assignTask_shouldThrow_whenTaskNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.assignTask(99L, 300L))
                .isInstanceOf(RuntimeException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteTask
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteTask — supprime la tâche et recalcule le completionRate")
    void deleteTask_shouldDeleteAndRecalculate() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.countTotalTasksByProjectId(1L)).thenReturn(0);
        when(taskRepository.countCompletedTasksByProjectId(1L)).thenReturn(0);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);

        taskService.deleteTask(10L);

        verify(taskRepository).delete(sampleTask);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("deleteTask — completionRate = 0 quand plus aucune tâche")
    void deleteTask_shouldSetZeroPercent_whenNoTasksLeft() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.countTotalTasksByProjectId(1L)).thenReturn(0);
        when(taskRepository.countCompletedTasksByProjectId(1L)).thenReturn(0);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project saved = inv.getArgument(0);
            assertThat(saved.getCompletionRate()).isEqualTo(0);
            return saved;
        });

        taskService.deleteTask(10L);
    }

    @Test
    @DisplayName("deleteTask — lève RuntimeException si tâche introuvable")
    void deleteTask_shouldThrow_whenNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(taskRepository, never()).delete(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private TaskDTO buildTaskDTO() {
        TaskDTO dto = new TaskDTO();
        dto.setId(10L);
        dto.setTitle("Tâche principale");
        dto.setDescription("Description de la tâche");
        dto.setStatus(TaskStatus.TODO);
        dto.setPriority(TaskPriority.HIGH);
        dto.setProjectId(1L);
        dto.setAssigneeId(200L);
        dto.setDeadline(LocalDate.of(2024, 6, 30));
        dto.setEstimatedHours(8);
        dto.setActualHours(0);
        return dto;
    }
}
