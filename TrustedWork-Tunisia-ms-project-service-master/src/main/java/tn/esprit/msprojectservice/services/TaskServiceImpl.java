package tn.esprit.msprojectservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.msprojectservice.dto.TaskDTO;
import tn.esprit.msprojectservice.entities.Project;
import tn.esprit.msprojectservice.entities.Task;
import tn.esprit.msprojectservice.entities.TaskStatus;
import tn.esprit.msprojectservice.repositories.IProjectRepository;
import tn.esprit.msprojectservice.repositories.ITaskRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements ITaskService {

    @Autowired
    private ITaskRepository taskRepository;

    @Autowired
    private IProjectRepository projectRepository;

    @Override
    public TaskDTO createTask(Long projectId, TaskDTO taskDTO) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'id : " + projectId));

        Task task = TaskDTO.toEntity(taskDTO);
        task.setProject(project);

        Task saved = taskRepository.save(task);

        // Recalculer le taux de complétion du projet
        updateProjectCompletionRate(projectId);

        return TaskDTO.fromEntity(saved);
    }

    @Override
    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tâche non trouvée avec l'id : " + id));
        return TaskDTO.fromEntity(task);
    }

    @Override
    public List<TaskDTO> getTasksByProjectId(Long projectId) {
        return taskRepository.findByProjectId(projectId)
                .stream()
                .map(TaskDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tâche non trouvée avec l'id : " + id));

        existing.setTitle(taskDTO.getTitle());
        existing.setDescription(taskDTO.getDescription());
        existing.setPriority(taskDTO.getPriority());
        existing.setDeadline(taskDTO.getDeadline());
        existing.setEstimatedHours(taskDTO.getEstimatedHours());
        existing.setActualHours(taskDTO.getActualHours());

        Task updated = taskRepository.save(existing);
        return TaskDTO.fromEntity(updated);
    }

    @Override
    public TaskDTO updateTaskStatus(Long id, TaskStatus status) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tâche non trouvée avec l'id : " + id));

        existing.setStatus(status);
        Task updated = taskRepository.save(existing);

        // Recalculer le taux de complétion du projet après changement de statut
        updateProjectCompletionRate(existing.getProject().getId());

        return TaskDTO.fromEntity(updated);
    }

    @Override
    public TaskDTO assignTask(Long id, Long assigneeId) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tâche non trouvée avec l'id : " + id));

        existing.setAssigneeId(assigneeId);
        Task updated = taskRepository.save(existing);
        return TaskDTO.fromEntity(updated);
    }

    @Override
    public void deleteTask(Long id) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tâche non trouvée avec l'id : " + id));

        Long projectId = existing.getProject().getId();
        taskRepository.delete(existing);

        // Recalculer le taux de complétion après suppression
        updateProjectCompletionRate(projectId);
    }

    // --- Méthode utilitaire : recalcul du completionRate du projet ---
    private void updateProjectCompletionRate(Long projectId) {
        int total = taskRepository.countTotalTasksByProjectId(projectId);
        int completed = taskRepository.countCompletedTasksByProjectId(projectId);

        int rate = (total > 0) ? (completed * 100 / total) : 0;

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'id : " + projectId));

        project.setCompletionRate(rate);
        projectRepository.save(project);
    }
}