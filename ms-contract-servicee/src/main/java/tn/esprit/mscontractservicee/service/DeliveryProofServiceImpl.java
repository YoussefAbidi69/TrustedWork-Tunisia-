package tn.esprit.mscontractservicee.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.mscontractservicee.dto.DeliveryProofResponse;
import tn.esprit.mscontractservicee.entity.DeliveryProof;
import tn.esprit.mscontractservicee.repository.DeliveryProofRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryProofServiceImpl implements IDeliveryProofService {

    private final DeliveryProofRepository deliveryProofRepository;

    @Override
    public Optional<DeliveryProofResponse> findForMilestone(Long milestoneId) {
        if (milestoneId == null) {
            return Optional.empty();
        }
        return deliveryProofRepository.findByMilestoneId(milestoneId).map(DeliveryProofServiceImpl::toResponse);
    }

    private static DeliveryProofResponse toResponse(DeliveryProof proof) {
        return new DeliveryProofResponse(
                proof.getId(),
                proof.getMilestoneId(),
                proof.getFichiers(),
                proof.getLienDemo(),
                proof.getRepoGit(),
                proof.getCommentaire(),
                proof.getHashMD5(),
                proof.getSubmittedAt(),
                proof.getStatus(),
                proof.getApprovedAt(),
                proof.getApprovedByCin()
        );
    }
}
