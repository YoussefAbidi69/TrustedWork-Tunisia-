package tn.esprit.freelancerprofileservice.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.freelancerprofileservice.entities.SchedulerConfig;
import tn.esprit.freelancerprofileservice.repositories.SchedulerConfigRepository;
import tn.esprit.freelancerprofileservice.services.ISchedulerConfigService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implémentation du service de configuration dynamique des schedulers.
 * Gère la lecture et la mise à jour des configs en base MySQL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfigServiceImpl implements ISchedulerConfigService {

    private final SchedulerConfigRepository schedulerConfigRepository;

    /**
     * Retourne toutes les configurations de schedulers.
     */
    @Override
    public List<SchedulerConfig> getAllConfigs() {
        log.debug("[SchedulerConfig] Lecture de toutes les configurations.");
        return schedulerConfigRepository.findAll();
    }

    /**
     * Retourne la configuration d'un job par son nom.
     * Lance une exception si le job est introuvable.
     */
    @Override
    public SchedulerConfig getConfigByJobName(String jobName) {
        return schedulerConfigRepository.findByJobName(jobName)
                .orElseThrow(() -> {
                    log.error("[SchedulerConfig] Job introuvable : {}", jobName);
                    return new RuntimeException("Scheduler introuvable : " + jobName);
                });
    }

    /**
     * Met à jour la configuration d'un job existant.
     * Seuls cronExpression, intervalMinutes et enabled sont modifiables.
     * Le champ updatedAt est mis à jour automatiquement via @PreUpdate.
     */
    @Override
    @Transactional
    public SchedulerConfig updateConfig(String jobName, SchedulerConfig updatedConfig) {
        // Récupération de la config existante
        SchedulerConfig existing = getConfigByJobName(jobName);

        // Mise à jour des champs modifiables uniquement
        existing.setCronExpression(updatedConfig.getCronExpression());
        existing.setIntervalMinutes(updatedConfig.getIntervalMinutes());
        existing.setEnabled(updatedConfig.getEnabled());

        SchedulerConfig saved = schedulerConfigRepository.save(existing);

        log.info("[SchedulerConfig] Mise à jour du job '{}' : cron={}, interval={}min, enabled={}",
                jobName,
                saved.getCronExpression(),
                saved.getIntervalMinutes(),
                saved.getEnabled());

        return saved;
    }

    /**
     * Enregistre l'heure de la dernière exécution d'un job.
     * Appelé par le meta-scheduler après chaque exécution réussie.
     */
    @Override
    @Transactional
    public void markLastRun(String jobName) {
        schedulerConfigRepository.findByJobName(jobName).ifPresent(config -> {
            config.setLastRun(LocalDateTime.now());
            schedulerConfigRepository.save(config);
            log.debug("[SchedulerConfig] lastRun mis à jour pour le job '{}'.", jobName);
        });
    }
}
