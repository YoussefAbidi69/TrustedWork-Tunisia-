package tn.esprit.userservice.service;


import tn.esprit.userservice.entity.Task;
import tn.esprit.userservice.entity.TaskPriority;
import tn.esprit.userservice.entity.TaskStatus;

import java.util.List;

public interface ITaskServices {

    Task createTask(Long projectId, Task task);

    List<Task> getTasksByProject(Long projectId);

    List<Task> getTasksByStatus(Long projectId, TaskStatus status);

    List<Task> getTasksByPriority(Long projectId, TaskPriority priority);

    List<Task> getOverdueTasks();

    Task getTaskById(Long id);

    Task updateTaskStatus(Long taskId, TaskStatus status);

    void deleteTask(Long id);

    Task updateTask(Long taskId, Task updatedTask);
}