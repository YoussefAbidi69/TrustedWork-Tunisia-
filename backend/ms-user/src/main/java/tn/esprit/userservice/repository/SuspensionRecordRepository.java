package tn.esprit.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.userservice.entity.SuspensionRecord;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuspensionRecordRepository extends JpaRepository<SuspensionRecord, Long> {

    // Historique complet des suspensions d'un utilisateur
    List<SuspensionRecord> findByUserIdOrderBySuspendedAtDesc(Long userId);

    // Suspension active en cours pour un utilisateur
    Optional<SuspensionRecord> findByUserIdAndActiveTrue(Long userId);

    // Toutes les suspensions actives — vue admin
    List<SuspensionRecord> findByActiveTrueOrderBySuspendedAtDesc();
}