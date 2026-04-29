package tn.esprit.smartjobboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.smartjobboard.entity.SkillCooccurrence;

import java.util.List;
import java.util.Optional;

public interface SkillCooccurrenceRepository extends JpaRepository<SkillCooccurrence, Long> {

    List<SkillCooccurrence> findBySkillPrimaryIn(List<String> skills);

    Optional<SkillCooccurrence> findBySkillPrimaryAndSkillRelated(String skillPrimary, String skillRelated);
}
