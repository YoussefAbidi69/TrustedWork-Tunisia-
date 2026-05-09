package tn.esprit.userservice.service;

import tn.esprit.userservice.dto.KycRequestDTO;
import tn.esprit.userservice.dto.KycReviewRequest;
import tn.esprit.userservice.dto.KycSubmitRequest;

import java.util.List;

public interface IKycRequestService {

    /**
     * Soumet une nouvelle demande KYC pour un utilisateur.
     */
    KycRequestDTO submitKycRequest(Long userId, KycSubmitRequest request);

    /**
     * Traite une demande KYC (APPROVED ou REJECTED) par un admin.
     */
    KycRequestDTO reviewKycRequest(Long kycRequestId, KycReviewRequest request, String adminEmail);

    /**
     * Retourne toutes les demandes KYC en attente (IN_REVIEW).
     */
    List<KycRequestDTO> getPendingRequests();

    /**
     * Retourne l'historique complet des demandes KYC d'un utilisateur.
     */
    List<KycRequestDTO> getHistoryByUser(Long userId);

    /**
     * Simule la liveness detection — compare selfie et photo CIN.
     * Retourne un score entre 0.0 et 1.0.
     */
    double computeLivenessScore(String cinImagePath, String selfiePath);
}