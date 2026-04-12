package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.freelancerprofileservice.entities.Endorsement;

import java.util.List;

public interface EndorsementRepository extends JpaRepository<Endorsement, Long> {

    List<Endorsement> findBySkillId(Long skillId);

    // Vérification anti-spam : un seul endorsement par (user, skill)
    boolean existsByEndorserIdAndSkillId(Long endorserId, Long skillId);

    // Nombre d'endorsements d'un skill (pour SkillAuthenticity)
    long countBySkillId(Long skillId);
}