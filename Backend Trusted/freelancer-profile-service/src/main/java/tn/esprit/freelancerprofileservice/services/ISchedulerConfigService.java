package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.entities.SchedulerConfig;

import java.util.List;

/**
 * Interface du service de gestion des configurations de schedulers.
 * Expose les opérations CRUD utilisées par le controller REST et le meta-scheduler.
 */
public interface ISchedulerConfigService {

    /**
     * Retourne toutes les configurations de schedulers (actives et inactives).
     *
     * @return liste complète des SchedulerConfig
     */
    List<SchedulerConfig> getAllConfigs();

    /**
     * Retourne la configuration d'un job par son nom.
     *
     * @param jobName nom du job (ex: "recalculateAllSkillScores")
     * @return SchedulerConfig correspondant
     * @throws RuntimeException si le job n'existe pas
     */
    SchedulerConfig getConfigByJobName(String jobName);

    /**
     * Met à jour la configuration d'un job existant.
     * Seuls les champs modifiables sont mis à jour : cronExpression, intervalMinutes, enabled.
     *
     * @param jobName     nom du job à modifier
     * @param updatedConfig objet contenant les nouvelles valeurs
     * @return SchedulerConfig mis à jour
     */
    SchedulerConfig updateConfig(String jobName, SchedulerConfig updatedConfig);

    /**
     * Met à jour le champ lastRun d'un job (appelé après chaque exécution réussie).
     *
     * @param jobName nom du job
     */
    void markLastRun(String jobName);
}
