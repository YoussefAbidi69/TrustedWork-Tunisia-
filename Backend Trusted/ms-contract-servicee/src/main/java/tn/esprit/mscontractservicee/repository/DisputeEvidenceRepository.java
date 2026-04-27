package tn.esprit.mscontractservicee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.mscontractservicee.entity.DisputeEvidence;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisputeEvidenceRepository extends JpaRepository<DisputeEvidence, Long> {
    List<DisputeEvidence> findByDisputeIdOrderByCreatedAtDesc(Long disputeId);
    Optional<DisputeEvidence> findByIdAndDisputeId(Long id, Long disputeId);
}

