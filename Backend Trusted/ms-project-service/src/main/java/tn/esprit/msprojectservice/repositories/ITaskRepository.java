package tn.esprit.msprojectservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.msprojectservice.entities.Task;
import tn.esprit.msprojectservice.entities.TaskStatus;

import java.util.List;

public interface ITaskRepository extends JpaRepository<Task, Long> {

    // Toutes les tâches d'un projet
    List<Task> findByProjectId(Long projectId);

    // Tâches d'un projet filtrées par statut (utile pour le Kanban)
    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);

    // Tâches assignées à un utilisateur
    List<Task> findByAssigneeId(Long assigneeId);

    // Compter les tâches DONE d'un projet (pour le completionRate)
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status = 'DONE'")
    int countCompletedTasksByProjectId(@Param("projectId") Long projectId);

    // Compter le total des tâches d'un projet
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId")
    int countTotalTasksByProjectId(@Param("projectId") Long projectId);

    // Tâches bloquées (IN_PROGRESS ou IN_REVIEW) — utile pour le scheduler IA Bottleneck
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND (t.status = 'IN_PROGRESS' OR t.status = 'IN_REVIEW')")
    List<Task> findBlockedTasksByProjectId(@Param("projectId") Long projectId);
}