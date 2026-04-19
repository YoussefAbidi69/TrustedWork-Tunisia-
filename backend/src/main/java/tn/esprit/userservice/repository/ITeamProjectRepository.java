package tn.esprit.userservice.repository;



import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.userservice.entity.ProjectStatus;
import tn.esprit.userservice.entity.TeamProject;

import java.util.List;
import java.util.Optional;

public interface ITeamProjectRepository extends JpaRepository<TeamProject, Long> {

  List<TeamProject> findByAgencyId(Long agencyId);

  List<TeamProject> findByAgencyIdAndActiveTrue(Long agencyId);

  List<TeamProject> findByAgencyIdAndStatus(Long agencyId, ProjectStatus status);

  Optional<TeamProject> findByContractId(Long contractId);
}