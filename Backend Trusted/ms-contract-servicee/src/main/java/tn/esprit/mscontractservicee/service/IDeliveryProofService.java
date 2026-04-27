package tn.esprit.mscontractservicee.service;

import tn.esprit.mscontractservicee.dto.DeliveryProofResponse;

import java.util.Optional;

public interface IDeliveryProofService {
    Optional<DeliveryProofResponse> findForMilestone(Long milestoneId);
}

