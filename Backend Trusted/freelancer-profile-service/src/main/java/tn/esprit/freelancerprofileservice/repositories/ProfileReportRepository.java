package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.freelancerprofileservice.entities.ProfileReport;
import tn.esprit.freelancerprofileservice.enums.ReportStatus;

import java.util.List;

@Repository
public interface ProfileReportRepository extends JpaRepository<ProfileReport, Long> {

    /**
     * Vérifie si un utilisateur a déjà signalé ce profil.
     */
    boolean existsByReporterIdAndProfileId(Long reporterId, Long profileId);

    /**
     * Liste tous les signalements d'un profil.
     */
    List<ProfileReport> findByProfileId(Long profileId);

    /**
     * Liste tous les signalements d'un profil triés du plus récent au plus ancien.
     */
    List<ProfileReport> findByProfileIdOrderByCreatedAtDesc(Long profileId);

    /**
     * Liste tous les signalements par statut.
     */
    List<ProfileReport> findByStatus(ReportStatus status);

    /**
     * Liste tous les signalements par statut triés du plus récent au plus ancien.
     */
    List<ProfileReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    /**
     * Liste complète des signalements triés du plus récent au plus ancien.
     */
    List<ProfileReport> findAllByOrderByCreatedAtDesc();

    /**
     * Nombre total de signalements pour un profil.
     */
    long countByProfileId(Long profileId);

    /**
     * Nombre de signalements d'un profil par statut.
     */
    long countByProfileIdAndStatus(Long profileId, ReportStatus status);

    /**
     * Nombre total de signalements créés par un utilisateur.
     */
    long countByReporterId(Long reporterId);
}