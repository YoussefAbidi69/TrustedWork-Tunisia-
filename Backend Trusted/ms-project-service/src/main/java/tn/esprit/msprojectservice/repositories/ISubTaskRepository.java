package tn.esprit.msprojectservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.msprojectservice.entities.SubTask;

import java.util.List;

public interface ISubTaskRepository extends JpaRepository<SubTask, Long> {

    // Toutes les sous-tâches d'une tâche
    List<SubTask> findByTaskId(Long taskId);

    // Compter les sous-tâches terminées d'une tâche
    @Query("SELECT COUNT(st) FROM SubTask st WHERE st.task.id = :taskId AND st.done = true")
    int countCompletedByTaskId(@Param("taskId") Long taskId);

    // Compter le total des sous-tâches d'une tâche
    @Query("SELECT COUNT(st) FROM SubTask st WHERE st.task.id = :taskId")
    int countTotalByTaskId(@Param("taskId") Long taskId);
}