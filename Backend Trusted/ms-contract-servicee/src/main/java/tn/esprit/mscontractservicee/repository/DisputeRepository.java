package tn.esprit.mscontractservicee.repository;

import tn.esprit.mscontractservicee.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.mscontractservicee.enums.DisputeStatus;

import java.util.Collection;
import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByContractIdOrderByOpenedAtDesc(Long contractId);
    List<Dispute> findByMilestoneIdOrderByOpenedAtDesc(Long milestoneId);
    boolean existsByMilestoneIdAndStatusIn(Long milestoneId, Collection<DisputeStatus> statuses);
    boolean existsByContractIdAndStatusIn(Long contractId, Collection<DisputeStatus> statuses);
    boolean existsByContractIdAndMilestoneIdIsNullAndStatusIn(Long contractId, Collection<DisputeStatus> statuses);
}
