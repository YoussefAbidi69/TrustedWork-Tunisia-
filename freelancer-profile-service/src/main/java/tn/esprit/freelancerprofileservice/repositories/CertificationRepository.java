package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.freelancerprofileservice.entities.Certification;

import java.time.LocalDate;
import java.util.List;

public interface CertificationRepository extends JpaRepository<Certification, Long> {

    List<Certification> findByProfileId(Long profileId);

    // Certifications qui expirent dans les 30 prochains jours (scheduler)
    @Query("SELECT c FROM Certification c WHERE c.expiryDate IS NOT NULL AND c.expiryDate <= :deadline AND c.isExpired = false")
    List<Certification> findExpiringCertifications(LocalDate deadline);

    // Certifications valides d'un profil
    List<Certification> findByProfileIdAndIsExpiredFalse(Long profileId);

    long countByProfileId(Long profileId);
}