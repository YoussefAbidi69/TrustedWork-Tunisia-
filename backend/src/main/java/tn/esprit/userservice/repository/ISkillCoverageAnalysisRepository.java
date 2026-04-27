package tn.esprit.userservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.userservice.entity.SkillCoverageAnalysis;

import java.util.List;
import java.util.Optional;

public interface ISkillCoverageAnalysisRepository extends JpaRepository<SkillCoverageAnalysis, Long> {

  List<SkillCoverageAnalysis> findByAgencyIdOrderByAnalyzedAtDesc(Long agencyId);

  Optional<SkillCoverageAnalysis> findFirstByAgencyIdOrderByAnalyzedAtDesc(Long agencyId);
}