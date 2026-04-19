package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.TaskCommentRequestDto;
import tn.esprit.userservice.dto.TaskCommentResponseDto;
import tn.esprit.userservice.entity.TaskComment;
import tn.esprit.userservice.mapper.TaskCommentMapper;
import tn.esprit.userservice.service.ITaskCommentServices;

import java.util.List;

@RestController
@RequestMapping("/task-comments")
@RequiredArgsConstructor
public class TaskCommentController {

    private final ITaskCommentServices taskCommentService;
    private final TaskCommentMapper taskCommentMapper;

    // ADD COMMENT TO TASK
    @PostMapping("/task/{taskId}")
    public TaskCommentResponseDto addComment(
            @PathVariable Long taskId,
            @RequestBody TaskCommentRequestDto dto
    ) {
        TaskComment comment = taskCommentMapper.toEntity(dto);
        TaskComment savedComment = taskCommentService.addComment(taskId, comment);
        return taskCommentMapper.toResponseDto(savedComment);
    }

    // GET COMMENTS BY TASK
    @GetMapping("/task/{taskId}")
    public List<TaskCommentResponseDto> getCommentsByTask(@PathVariable Long taskId) {
        return taskCommentService.getCommentsByTask(taskId)
                .stream()
                .map(taskCommentMapper::toResponseDto)
                .toList();
    }

    // GET COMMENTS BY USER
    @GetMapping("/user/{userId}")
    public List<TaskCommentResponseDto> getCommentsByUser(@PathVariable Long userId) {
        return taskCommentService.getCommentsByUser(userId)
                .stream()
                .map(taskCommentMapper::toResponseDto)
                .toList();
    }

    // GET COMMENT BY ID
    @GetMapping("/{commentId}")
    public TaskCommentResponseDto getCommentById(@PathVariable Long commentId) {
        TaskComment comment = taskCommentService.getCommentById(commentId);
        return taskCommentMapper.toResponseDto(comment);
    }

    // DELETE COMMENT
    @DeleteMapping("/{commentId}")
    public void deleteComment(@PathVariable Long commentId) {
        taskCommentService.deleteComment(commentId);
    }
}