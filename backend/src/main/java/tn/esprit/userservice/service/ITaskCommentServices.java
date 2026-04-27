package tn.esprit.userservice.service;


import tn.esprit.userservice.entity.TaskComment;

import java.util.List;

public interface ITaskCommentServices {

    TaskComment addComment(Long taskId, TaskComment comment);

    List<TaskComment> getCommentsByTask(Long taskId);

    List<TaskComment> getCommentsByUser(Long userId);

    TaskComment getCommentById(Long commentId);

    void deleteComment(Long commentId);
}