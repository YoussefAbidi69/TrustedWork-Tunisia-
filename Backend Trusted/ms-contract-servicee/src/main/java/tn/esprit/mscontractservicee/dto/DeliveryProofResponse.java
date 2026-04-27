package tn.esprit.mscontractservicee.dto;

import tn.esprit.mscontractservicee.enums.DeliveryStatus;

import java.time.LocalDateTime;

public record DeliveryProofResponse(
        Long id,
        Long milestoneId,
        String fichiers,
        String lienDemo,
        String repoGit,
        String commentaire,
        String hashMD5,
        LocalDateTime submittedAt,
        DeliveryStatus status,
        LocalDateTime approvedAt,
        Long approvedByCin
) {
}
