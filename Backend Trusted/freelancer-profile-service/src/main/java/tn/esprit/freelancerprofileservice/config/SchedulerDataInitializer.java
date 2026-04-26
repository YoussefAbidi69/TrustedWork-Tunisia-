package tn.esprit.freelancerprofileservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.esprit.freelancerprofileservice.entities.SchedulerConfig;
import tn.esprit.freelancerprofileservice.repositories.SchedulerConfigRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Initialisation des configurations par défaut des schedulers.
 *
 * Exécuté au démarrage du service (CommandLineRunner).
 * Logique idempotente : n'insère une config que si elle n'existe pas encore.
 * → Aucun risque de doublon même après redémarrage.
 *
 * Les 4 jobs correspondent aux méthodes de ProfileScheduler.java :
 *   1. recalculateAllSkillScores   → nuit à 1h00         (toutes les 24h)
 *   2. updateRegionalRankings      → lundi à minuit       (toutes les 168h)
 *   3. sendProfileCompletionReminders → matin à 9h        (toutes les 24h)
 *   4. checkCertificationExpiry    → 1er du mois à minuit (toutes les 720h)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulerDataInitializer implements CommandLineRunner {

    private final SchedulerConfigRepository schedulerConfigRepository;

    @Override
    public void run(String... args) {
        log.info("[SchedulerInit] Vérification des configurations par défaut...");

        // Définition des 4 configs par défaut
        List<SchedulerConfig> defaultConfigs = List.of(

            SchedulerConfig.builder()
                .jobName("recalculateAllSkillScores")
                .description("Recalcul nocturne des scores d'authenticité des compétences")
                .cronExpression("0 0 1 * * *")
                .intervalMinutes(1440)       // 24h = 1440 min
                .enabled(true)
                .lastRun(null)
                .updatedAt(LocalDateTime.now())
                .build(),

            SchedulerConfig.builder()
                .jobName("updateRegionalRankings")
                .description("Mise à jour hebdomadaire du classement régional des freelancers")
                .cronExpression("0 0 0 * * MON")
                .intervalMinutes(10080)      // 7 jours = 10 080 min
                .enabled(true)
                .lastRun(null)
                .updatedAt(LocalDateTime.now())
                .build(),

            SchedulerConfig.builder()
                .jobName("sendProfileCompletionReminders")
                .description("Rappels par email aux freelancers avec profil incomplet (< 60%)")
                .cronExpression("0 0 9 * * *")
                .intervalMinutes(1440)       // 24h = 1440 min
                .enabled(true)
                .lastRun(null)
                .updatedAt(LocalDateTime.now())
                .build(),

            SchedulerConfig.builder()
                .jobName("checkCertificationExpiry")
                .description("Vérification mensuelle des certifications expirées ou proches de l'expiration")
                .cronExpression("0 0 0 1 * *")
                .intervalMinutes(43200)      // 30 jours = 43 200 min
                .enabled(true)
                .lastRun(null)
                .updatedAt(LocalDateTime.now())
                .build()
        );

        // Insertion idempotente : on vérifie avant d'insérer
        int inserted = 0;
        for (SchedulerConfig config : defaultConfigs) {
            boolean alreadyExists = schedulerConfigRepository
                    .findByJobName(config.getJobName())
                    .isPresent();

            if (!alreadyExists) {
                schedulerConfigRepository.save(config);
                log.info("[SchedulerInit] ✔ Config insérée : '{}' (interval={}min, enabled={})",
                        config.getJobName(),
                        config.getIntervalMinutes(),
                        config.getEnabled());
                inserted++;
            } else {
                log.debug("[SchedulerInit] Config déjà existante, ignorée : '{}'", config.getJobName());
            }
        }

        if (inserted == 0) {
            log.info("[SchedulerInit] Toutes les configs sont déjà initialisées. Aucune insertion.");
        } else {
            log.info("[SchedulerInit] {} config(s) insérée(s) avec succès.", inserted);
        }
    }
}
