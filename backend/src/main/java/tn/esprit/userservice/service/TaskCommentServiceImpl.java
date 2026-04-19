package tn.esprit.userservice.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.entity.Task;
import tn.esprit.userservice.entity.TaskComment;
import tn.esprit.userservice.repository.ITaskCommentRepository;
import tn.esprit.userservice.repository.ITaskRepository;
import tn.esprit.userservice.service.ITaskCommentServices;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskCommentServiceImpl implements ITaskCommentServices {

    private final ITaskCommentRepository taskCommentRepository;
    private final ITaskRepository taskRepository;

    @Override
    public TaskComment addComment(Long taskId, TaskComment comment) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        comment.setTask(task);
        comment.setCommentedAt(LocalDateTime.now());

        return taskCommentRepository.save(comment);
    }

    @Override
    public List<TaskComment> getCommentsByTask(Long taskId) {
        return taskCommentRepository.findByTaskIdOrderByCommentedAtAsc(taskId);
    }

    @Override
    public List<TaskComment> getCommentsByUser(Long userId) {
        return taskCommentRepository.findByUserIdOrderByCommentedAtDesc(userId);
    }

    @Override
    public TaskComment getCommentById(Long commentId) {
        return taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Task comment not found"));
    }

    @Override
    public void deleteComment(Long commentId) {
        taskCommentRepository.deleteById(commentId);
    }
}