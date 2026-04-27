package tn.esprit.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.userservice.entity.KycRequest;
import tn.esprit.userservice.entity.KycStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycRequestRepository extends JpaRepository<KycRequest, Long> {

    // Dernière demande KYC d'un utilisateur
    Optional<KycRequest> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    // Toutes les demandes d'un utilisateur — historique complet
    List<KycRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Demandes en attente de review admin
    List<KycRequest> findByStatusOrderByCreatedAtAsc(KycStatus status);

    // Vérifier si une demande active existe déjà
    boolean existsByUserIdAndStatus(Long userId, KycStatus status);
}