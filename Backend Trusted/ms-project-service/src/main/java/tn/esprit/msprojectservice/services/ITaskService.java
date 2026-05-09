package tn.esprit.msprojectservice.services;

import tn.esprit.msprojectservice.dto.TaskDTO;
import tn.esprit.msprojectservice.entities.TaskStatus;

import java.util.List;

public interface ITaskService {

    TaskDTO createTask(Long projectId, TaskDTO taskDTO);

    TaskDTO getTaskById(Long id);

    List<TaskDTO> getTasksByProjectId(Long projectId);

    TaskDTO updateTask(Long id, TaskDTO taskDTO);

    TaskDTO updateTaskStatus(Long id, TaskStatus status);

    TaskDTO assignTask(Long id, Long assigneeId);

    void deleteTask(Long id);
}