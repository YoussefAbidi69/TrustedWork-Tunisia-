package tn.esprit.community.contribution;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributionRepository extends JpaRepository<Contribution, Long> {
    Optional<Contribution> findByUserId(Long userId);
}
