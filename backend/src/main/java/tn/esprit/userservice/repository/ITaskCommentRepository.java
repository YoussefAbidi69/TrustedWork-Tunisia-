package tn.esprit.userservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.userservice.entity.TaskComment;

import java.util.List;

public interface ITaskCommentRepository extends JpaRepository<TaskComment, Long> {

    List<TaskComment> findByTaskIdOrderByCommentedAtAsc(Long taskId);

    List<TaskComment> findByUserIdOrderByCommentedAtDesc(Long userId);
}