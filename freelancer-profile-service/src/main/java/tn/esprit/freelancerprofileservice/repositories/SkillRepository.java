package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.enums.SkillCategory;
import tn.esprit.freelancerprofileservice.enums.SkillLevel;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findByProfileIdOrderByAuthenticityScoreDesc(Long profileId);

    List<Skill> findByProfileIdAndLevelOrderByAuthenticityScoreDesc(Long profileId, SkillLevel level);

    List<Skill> findByProfileIdAndCategoryOrderByAuthenticityScoreDesc(Long profileId, SkillCategory category);

    Optional<Skill> findByProfileIdAndNormalizedName(Long profileId, String normalizedName);

    boolean existsByProfileIdAndNormalizedName(Long profileId, String normalizedName);

    long countByProfileId(Long profileId);

    @Query("""
            SELECT s.name, COUNT(s) as cnt
            FROM Skill s
            GROUP BY s.name
            ORDER BY cnt DESC
            """)
    List<Object[]> findTopSkillsRaw();

    @Query("""
            SELECT s.category, COUNT(s)
            FROM Skill s
            GROUP BY s.category
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> countSkillsByCategory();

    @Query("""
            SELECT COUNT(DISTINCT s.profile.id)
            FROM Skill s
            WHERE s.normalizedName = :normalizedName
            """)
    long countDistinctProfilesByNormalizedName(String normalizedName);


    @Query("SELECT COALESCE(SUM(s.endorsementCount), 0) FROM Skill s WHERE s.profile.id = :profileId")
    long sumEndorsementsByProfileId(@Param("profileId") Long profileId);
}