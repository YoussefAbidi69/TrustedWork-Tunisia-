package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.freelancerprofileservice.entities.PortfolioItem;

import java.util.List;
import java.util.Optional;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    List<PortfolioItem> findByProfileIdOrderByPinnedDescCompletionDateDescIdDesc(Long profileId);

    List<PortfolioItem> findByProfileIdAndPinnedTrueOrderByCompletionDateDescIdDesc(Long profileId);

    long countByProfileId(Long profileId);

    long countByProfileIdAndPinnedTrue(Long profileId);

    boolean existsByProfileIdAndTitleIgnoreCase(Long profileId, String title);

    boolean existsByProfileIdAndTitleIgnoreCaseAndIdNot(Long profileId, String title, Long id);

    Optional<PortfolioItem> findByIdAndProfileId(Long id, Long profileId);
}