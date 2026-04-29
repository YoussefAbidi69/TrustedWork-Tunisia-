package tn.esprit.mscontractservicee.service;

import tn.esprit.mscontractservicee.dto.DeliveryProofSubmitRequest;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.enums.MilestoneStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface IMilestoneService {

    Milestone createMilestone(Milestone milestone);

    Milestone updateMilestone(Long id, Milestone milestone);

    Optional<Milestone> findById(Long id);


    Page<Milestone> findAll(Pageable pageable);

    Milestone updateStatus(Long id, MilestoneStatus status);

    Milestone startMilestone(Long id);

    Milestone submitMilestone(Long id);

    /**
     * Submit milestone with its delivery proof (atomic / transactional).
     */
    Milestone submitMilestoneWithProof(Long id, DeliveryProofSubmitRequest proof);

    Milestone approveMilestone(Long id, Long approvedByCin);

    Milestone autoApproveMilestone(Long id, Long approvedByCin);

    void deleteMilestone(Long id);

    /**
     * Allows client/admin to update only the deadline of a rejected milestone (REJECTED -> REJECTED).
     */
    Milestone updateRejectedMilestoneDeadline(Long id, LocalDate newDeadline);

    /**
     * Rejects a submitted milestone and stores the rejection reason.
     * Optionally allows the client/admin to set a new deadline so the freelancer can rework and re-submit.
     */
    Milestone rejectMilestone(Long id, String rejectionReason, LocalDate newDeadline);

    default Milestone rejectMilestone(Long id, String rejectionReason) {
        return rejectMilestone(id, rejectionReason, null);
    }
    List<Milestone> findByContractId(Long contractId);

    List<Milestone> findForClientCin(Long clientCin);

    List<Milestone> findForSignedFreelancerCin(Long freelancerCin);

}
