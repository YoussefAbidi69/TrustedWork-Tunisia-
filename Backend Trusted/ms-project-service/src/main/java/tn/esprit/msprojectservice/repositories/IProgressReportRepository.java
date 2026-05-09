package tn.esprit.msprojectservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.msprojectservice.entities.ProgressReport;

import java.util.List;
import java.util.Optional;

public interface IProgressReportRepository extends JpaRepository<ProgressReport, Long> {

    // Historique de tous les rapports d'un projet (ordre chronologique décroissant)
    List<ProgressReport> findByProjectIdOrderByGeneratedAtDesc(Long projectId);

    // Dernier rapport généré pour un projet
    @Query("SELECT pr FROM ProgressReport pr WHERE pr.project.id = :projectId ORDER BY pr.generatedAt DESC LIMIT 1")
    Optional<ProgressReport> findLatestByProjectId(@Param("projectId") Long projectId);
}