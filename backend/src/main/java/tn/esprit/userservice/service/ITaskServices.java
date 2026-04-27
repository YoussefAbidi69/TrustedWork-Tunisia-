package tn.esprit.userservice.service;

import tn.esprit.userservice.dto.TaskUpdateDto;
import tn.esprit.userservice.entity.Task;
import tn.esprit.userservice.entity.TaskStatus;

import java.util.List;

public interface ITaskServices {
    List<Task> getTasksByAgency(Long agencyId);
    List<Task> getTasksByProject(Long agencyId, Long projectId);
    Task createTask(Long agencyId, Long projectId, Long requesterId, Long assignedMemberId, Task task);
    Task updateTask(Long agencyId, Long taskId, Long requesterId, TaskUpdateDto dto);
    Task updateTaskStatus(Long agencyId, Long taskId, Long requesterId, TaskStatus status);
    void deleteTask(Long agencyId, Long taskId, Long requesterId);
}