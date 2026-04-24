package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.freelancerprofileservice.entities.Endorsement;

import java.util.List;
import java.util.Optional;

public interface EndorsementRepository extends JpaRepository<Endorsement, Long> {

    List<Endorsement> findBySkillIdOrderByEndorsedAtDesc(Long skillId);

    Optional<Endorsement> findByEndorserIdAndSkillId(Long endorserId, Long skillId);

    boolean existsByEndorserIdAndSkillId(Long endorserId, Long skillId);

    long countBySkillId(Long skillId);

    long countBySkillProfileId(Long profileId);

    @Query("""
            SELECT e.endorserId, COUNT(e)
            FROM Endorsement e
            GROUP BY e.endorserId
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> findTopEndorsersRaw();
}