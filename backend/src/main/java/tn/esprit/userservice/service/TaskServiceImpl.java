package tn.esprit.userservice.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.entity.Task;
import tn.esprit.userservice.entity.TaskPriority;
import tn.esprit.userservice.entity.TaskStatus;
import tn.esprit.userservice.entity.TeamProject;
import tn.esprit.userservice.repository.ITaskRepository;
import tn.esprit.userservice.repository.ITeamProjectRepository;
import tn.esprit.userservice.service.ITaskServices;
import tn.esprit.userservice.service.ITeamProjectServices;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements ITaskServices {

    private final ITaskRepository taskRepository;
    private final ITeamProjectRepository teamProjectRepository;
    private final ITeamProjectServices teamProjectService;
    @Override
    public Task createTask(Long projectId, Task task) {
        TeamProject project = teamProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        task.setProject(project);

        Task savedTask = taskRepository.save(task);

        teamProjectService.updateProjectProgress(projectId);

        return savedTask;
    }

    @Override
    public List<Task> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    @Override
    public List<Task> getTasksByStatus(Long projectId, TaskStatus status) {
        return taskRepository.findByProjectIdAndStatus(projectId, status);
    }

    @Override
    public List<Task> getTasksByPriority(Long projectId, TaskPriority priority) {
        return taskRepository.findByProjectIdAndPriority(projectId, priority);
    }

    @Override
    public List<Task> getOverdueTasks() {
        return taskRepository.findByDueDateBeforeAndStatusNot(LocalDateTime.now(), TaskStatus.DONE);
    }

    @Override
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }

    @Override
    public Task updateTaskStatus(Long taskId, TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setStatus(status);

        if (status == TaskStatus.DONE) {
            task.setCompletedAt(LocalDateTime.now());
        }

        Task savedTask = taskRepository.save(task);

        teamProjectService.updateProjectProgress(task.getProject().getId());

        return savedTask;
    }

    @Override
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        Long projectId = task.getProject().getId();

        taskRepository.delete(task);

        teamProjectService.updateProjectProgress(projectId);
    }

    @Override
    public Task updateTask(Long taskId, Task updatedTask) {
        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (updatedTask.getTitle() != null) {
            existingTask.setTitle(updatedTask.getTitle());
        }

        if (updatedTask.getDescription() != null) {
            existingTask.setDescription(updatedTask.getDescription());
        }

        if (updatedTask.getPriority() != null) {
            existingTask.setPriority(updatedTask.getPriority());
        }

        if (updatedTask.getStatus() != null) {
            existingTask.setStatus(updatedTask.getStatus());

            if (updatedTask.getStatus() == TaskStatus.DONE) {
                existingTask.setCompletedAt(LocalDateTime.now());
            }
        }

        if (updatedTask.getDueDate() != null) {
            existingTask.setDueDate(updatedTask.getDueDate());
        }

        Task savedTask = taskRepository.save(existingTask);

        teamProjectService.updateProjectProgress(existingTask.getProject().getId());

        return savedTask;
    }
}