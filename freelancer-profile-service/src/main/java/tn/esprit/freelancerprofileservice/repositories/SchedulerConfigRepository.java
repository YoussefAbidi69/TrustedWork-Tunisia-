package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.freelancerprofileservice.entities.SchedulerConfig;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour la configuration dynamique des schedulers.
 * Hérite des méthodes CRUD standard de JpaRepository.
 */
@Repository
public interface SchedulerConfigRepository extends JpaRepository<SchedulerConfig, Long> {

    /**
     * Recherche une config par son nom de job (identifiant unique).
     *
     * @param jobName nom du job (ex: "recalculateAllSkillScores")
     * @return Optional<SchedulerConfig>
     */
    Optional<SchedulerConfig> findByJobName(String jobName);

    /**
     * Retourne uniquement les configs dont le job est actif (enabled = true).
     * Utilisé par le meta-scheduler pour ne traiter que les jobs actifs.
     *
     * @return liste des configs actives
     */
    List<SchedulerConfig> findByEnabledTrue();
}
