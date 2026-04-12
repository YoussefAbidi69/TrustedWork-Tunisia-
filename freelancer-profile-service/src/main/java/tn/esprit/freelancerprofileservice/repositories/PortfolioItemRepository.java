package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.freelancerprofileservice.entities.PortfolioItem;

import java.util.List;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    List<PortfolioItem> findByProfileId(Long profileId);

    // Nombre de projets portfolio (utilisé pour SkillAuthenticity)
    long countByProfileId(Long profileId);
}