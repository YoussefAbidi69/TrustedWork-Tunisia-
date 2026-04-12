package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.enums.SkillLevel;

import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findByProfileId(Long profileId);

    List<Skill> findByProfileIdAndLevel(Long profileId, SkillLevel level);

    // Top 10 skills les plus présents sur la plateforme (pour SkillGapService)
    @Query("SELECT s.name, COUNT(s) as cnt FROM Skill s GROUP BY s.name ORDER BY cnt DESC")
    List<Object[]> findTopSkillsRaw();

    boolean existsByProfileIdAndName(Long profileId, String name);
}