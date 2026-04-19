package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.TaskRequestDto;
import tn.esprit.userservice.dto.TaskResponseDto;
import tn.esprit.userservice.dto.TaskUpdateDto;
import tn.esprit.userservice.entity.Task;
import tn.esprit.userservice.entity.TaskPriority;
import tn.esprit.userservice.entity.TaskStatus;
import tn.esprit.userservice.mapper.TaskMapper;
import tn.esprit.userservice.service.ITaskServices;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final ITaskServices taskService;
    private final TaskMapper taskMapper;

    // CREATE TASK IN PROJECT
    @PostMapping("/project/{projectId}")
    public TaskResponseDto createTask(@PathVariable Long projectId, @RequestBody TaskRequestDto dto) {
        Task task = taskMapper.toEntity(dto);
        Task savedTask = taskService.createTask(projectId, task);
        return taskMapper.toResponseDto(savedTask);
    }

    // GET ALL TASKS OF A PROJECT
    @GetMapping("/project/{projectId}")
    public List<TaskResponseDto> getTasksByProject(@PathVariable Long projectId) {
        return taskService.getTasksByProject(projectId)
                .stream()
                .map(taskMapper::toResponseDto)
                .toList();
    }

    // GET TASKS BY STATUS (KANBAN)
    @GetMapping("/project/{projectId}/status")
    public List<TaskResponseDto> getTasksByStatus(
            @PathVariable Long projectId,
            @RequestParam TaskStatus status
    ) {
        return taskService.getTasksByStatus(projectId, status)
                .stream()
                .map(taskMapper::toResponseDto)
                .toList();
    }

    // GET TASKS BY PRIORITY
    @GetMapping("/project/{projectId}/priority")
    public List<TaskResponseDto> getTasksByPriority(
            @PathVariable Long projectId,
            @RequestParam TaskPriority priority
    ) {
        return taskService.getTasksByPriority(projectId, priority)
                .stream()
                .map(taskMapper::toResponseDto)
                .toList();
    }

    // GET OVERDUE TASKS
    @GetMapping("/overdue")
    public List<TaskResponseDto> getOverdueTasks() {
        return taskService.getOverdueTasks()
                .stream()
                .map(taskMapper::toResponseDto)
                .toList();
    }

    // GET TASK BY ID
    @GetMapping("/{taskId}")
    public TaskResponseDto getTaskById(@PathVariable Long taskId) {
        Task task = taskService.getTaskById(taskId);
        return taskMapper.toResponseDto(task);
    }

    // UPDATE TASK
    @PutMapping("/{taskId}")
    public TaskResponseDto updateTask(@PathVariable Long taskId, @RequestBody TaskUpdateDto dto) {
        Task existingTask = taskService.getTaskById(taskId);
        taskMapper.updateEntityFromDto(dto, existingTask);
        Task updatedTask = taskService.updateTask(taskId, existingTask);
        return taskMapper.toResponseDto(updatedTask);
    }

    // UPDATE TASK STATUS (KANBAN DRAG & DROP)
    @PutMapping("/{taskId}/status")
    public TaskResponseDto updateTaskStatus(
            @PathVariable Long taskId,
            @RequestParam TaskStatus status
    ) {
        Task updatedTask = taskService.updateTaskStatus(taskId, status);
        return taskMapper.toResponseDto(updatedTask);
    }

    // DELETE TASK
    @DeleteMapping("/{taskId}")
    public void deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
    }
}