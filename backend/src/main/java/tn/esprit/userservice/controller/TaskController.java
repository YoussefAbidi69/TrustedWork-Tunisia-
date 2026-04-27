package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.TaskRequestDto;
import tn.esprit.userservice.dto.TaskResponseDto;
import tn.esprit.userservice.dto.TaskUpdateDto;
import tn.esprit.userservice.dto.TaskStatusUpdateDto;
import tn.esprit.userservice.entity.Task;
import tn.esprit.userservice.mapper.TaskMapper;
import tn.esprit.userservice.service.ITaskServices;

import java.util.List;

@RestController
@RequestMapping("/agencies/{agencyId}")
@RequiredArgsConstructor
public class TaskController {

    private final ITaskServices taskService;
    private final TaskMapper taskMapper;

    // GET /agencies/:id/tasks
    @GetMapping("/tasks")
    public ResponseEntity<List<TaskResponseDto>> getAllTasksByAgency(@PathVariable Long agencyId) {
        List<TaskResponseDto> tasks = taskService.getTasksByAgency(agencyId)
                .stream()
                .map(taskMapper::toResponseDto)
                .toList();
        return ResponseEntity.ok(tasks);
    }

    // GET /agencies/:id/projects/:projectId/tasks
    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponseDto>> getTasksByProject(
            @PathVariable Long agencyId,
            @PathVariable Long projectId) {
        List<TaskResponseDto> tasks = taskService.getTasksByProject(agencyId, projectId)
                .stream()
                .map(taskMapper::toResponseDto)
                .toList();
        return ResponseEntity.ok(tasks);
    }

    // POST /agencies/:id/tasks
    @PostMapping("/tasks")
    public ResponseEntity<TaskResponseDto> createTask(
            @PathVariable Long agencyId,
            @RequestBody TaskRequestDto dto) {
        Task task = taskMapper.toEntity(dto);
        Task savedTask = taskService.createTask(agencyId, dto.getProjectId(), dto.getRequesterId(), dto.getAssignedMemberId(), task);
        return ResponseEntity.ok(taskMapper.toResponseDto(savedTask));
    }

    // PATCH /agencies/:id/tasks/:taskId
    @PatchMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponseDto> updateTask(
            @PathVariable Long agencyId,
            @PathVariable Long taskId,
            @RequestBody TaskUpdateDto dto) {
        
        Task updatedTask = taskService.updateTask(agencyId, taskId, dto.getRequesterId(), dto);
        return ResponseEntity.ok(taskMapper.toResponseDto(updatedTask));
    }

    // PATCH /agencies/:id/tasks/:taskId/status
    @PatchMapping("/tasks/{taskId}/status")
    public ResponseEntity<TaskResponseDto> updateTaskStatus(
            @PathVariable Long agencyId,
            @PathVariable Long taskId,
            @RequestParam Long requesterId,
            @RequestBody TaskStatusUpdateDto dto) {
        Task updatedTask = taskService.updateTaskStatus(agencyId, taskId, requesterId, dto.getStatus());
        return ResponseEntity.ok(taskMapper.toResponseDto(updatedTask));
    }

    // DELETE /agencies/:id/tasks/:taskId
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long agencyId,
            @PathVariable Long taskId,
            @RequestParam Long requesterId) {
        taskService.deleteTask(agencyId, taskId, requesterId);
        return ResponseEntity.noContent().build();
    }
}