package tn.esprit.msprojectservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.msprojectservice.entities.DeliveryRiskSignal;
import tn.esprit.msprojectservice.entities.RiskType;

import java.util.List;
import java.util.Optional;

public interface IRiskSignalRepository extends JpaRepository<DeliveryRiskSignal, Long> {

    // Tous les signaux actifs (non résolus) d'un projet
    List<DeliveryRiskSignal> findByProjectIdAndResolvedFalse(Long projectId);

    // Tous les signaux d'un projet (actifs + résolus)
    List<DeliveryRiskSignal> findByProjectId(Long projectId);

    // Dernier signal le plus critique d'un projet (non résolu)
    @Query("SELECT drs FROM DeliveryRiskSignal drs WHERE drs.project.id = :projectId AND drs.resolved = false ORDER BY CASE drs.severity WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 WHEN 'LOW' THEN 4 END, drs.detectedAt DESC LIMIT 1")
    Optional<DeliveryRiskSignal> findLatestCriticalByProjectId(@Param("projectId") Long projectId);

    // Compter les risques actifs d'un projet (utilisé par ProgressReport)
    @Query("SELECT COUNT(drs) FROM DeliveryRiskSignal drs WHERE drs.project.id = :projectId AND drs.resolved = false")
    int countActiveRisksByProjectId(@Param("projectId") Long projectId);

    // Vérifier si un signal du même type existe déjà pour une tâche (éviter les doublons)
    @Query("SELECT COUNT(drs) > 0 FROM DeliveryRiskSignal drs WHERE drs.project.id = :projectId AND drs.riskType = :riskType AND drs.affectedTaskId = :taskId AND drs.resolved = false")
    boolean existsActiveSignal(@Param("projectId") Long projectId, @Param("riskType") RiskType riskType, @Param("taskId") Long taskId);

    // Vérifier si un signal INACTIVITY ou SCOPE_CREEP existe déjà pour le projet (pas lié à une tâche)
    @Query("SELECT COUNT(drs) > 0 FROM DeliveryRiskSignal drs WHERE drs.project.id = :projectId AND drs.riskType = :riskType AND drs.resolved = false")
    boolean existsActiveProjectSignal(@Param("projectId") Long projectId, @Param("riskType") RiskType riskType);
}