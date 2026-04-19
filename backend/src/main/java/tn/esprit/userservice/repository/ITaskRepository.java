package tn.esprit.userservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.userservice.entity.Task;
import tn.esprit.userservice.entity.TaskPriority;
import tn.esprit.userservice.entity.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface ITaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);

    List<Task> findByProjectIdAndPriority(Long projectId, TaskPriority priority);

    List<Task> findByDueDateBeforeAndStatusNot(LocalDateTime date, TaskStatus status);
}