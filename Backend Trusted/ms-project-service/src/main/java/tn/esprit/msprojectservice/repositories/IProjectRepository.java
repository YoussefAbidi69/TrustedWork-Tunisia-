package tn.esprit.msprojectservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.msprojectservice.entities.Project;
import tn.esprit.msprojectservice.entities.ProjectStatus;

import java.util.List;
import java.util.Optional;

public interface IProjectRepository extends JpaRepository<Project, Long> {

    // Trouver le projet lié à un contrat
    Optional<Project> findByContractId(Long contractId);

    // Tous les projets d'un utilisateur (client OU freelancer)
    @Query("SELECT p FROM Project p WHERE p.clientId = :userId OR p.freelancerId = :userId")
    List<Project> findAllByUserId(@Param("userId") Long userId);

    // Tous les projets par statut
    List<Project> findByStatus(ProjectStatus status);

    // Tous les projets actifs (utilisé par le scheduler IA)
    @Query("SELECT p FROM Project p WHERE p.status = 'ACTIVE'")
    List<Project> findAllActiveProjects();
}