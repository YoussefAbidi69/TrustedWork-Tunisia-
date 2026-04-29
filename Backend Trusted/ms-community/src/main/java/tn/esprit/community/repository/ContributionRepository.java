package tn.esprit.community.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.Contribution;

public interface ContributionRepository extends JpaRepository<Contribution, Long> {
    Optional<Contribution> findByUserId(Long userId);
}
