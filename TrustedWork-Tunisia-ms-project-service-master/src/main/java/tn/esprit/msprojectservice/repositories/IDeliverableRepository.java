package tn.esprit.msprojectservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.msprojectservice.entities.Deliverable;
import tn.esprit.msprojectservice.entities.DeliverableStatus;

import java.util.List;

public interface IDeliverableRepository extends JpaRepository<Deliverable, Long> {

    // Tous les livrables d'un projet
    List<Deliverable> findByProjectId(Long projectId);

    // Livrables filtrés par statut dans un projet
    List<Deliverable> findByProjectIdAndStatus(Long projectId, DeliverableStatus status);

    // Compter les livrables en attente de validation (SUBMITTED) pour un projet
    @Query("SELECT COUNT(d) FROM Deliverable d WHERE d.project.id = :projectId AND d.status = 'SUBMITTED'")
    int countOpenDeliverablesByProjectId(@Param("projectId") Long projectId);

    // Livrables liés à une tâche spécifique
    List<Deliverable> findByTaskId(Long taskId);
}