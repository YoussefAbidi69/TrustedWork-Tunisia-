package tn.esprit.freelancerprofileservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.freelancerprofileservice.clients.UserClient;
import tn.esprit.freelancerprofileservice.entities.Certification;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.SchedulerConfig;
import tn.esprit.freelancerprofileservice.repositories.CertificationRepository;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;
import tn.esprit.freelancerprofileservice.services.ICompletenessService;
import tn.esprit.freelancerprofileservice.services.IEmailService;
import tn.esprit.freelancerprofileservice.services.ISchedulerConfigService;
import tn.esprit.freelancerprofileservice.services.ISkillAuthenticityService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scheduler dynamique du Module 02 — 4 tâches planifiables depuis le backoffice.
 *
 * Fonctionnement :
 *   Un meta-scheduler tourne toutes les 60 secondes.
 *   À chaque tick, il relit la configuration depuis la base MySQL.
 *   Si un job est activé (enabled=true) et que son délai est écoulé
 *   (lastRun + intervalMinutes <= maintenant), il exécute le job
 *   et met à jour le champ lastRun.
 *
 * Avantage : intervalMinutes et enabled sont modifiables en temps réel
 *            depuis le backoffice Angular sans redémarrer le service.
 *
 * Noms de jobs (jobName en base) :
 *   1. recalculateAllSkillScores      → recalcul des scores d'authenticité
 *   2. updateRegionalRankings         → classement régional hebdomadaire
 *   3. sendProfileCompletionReminders → rappels profils incomplets
 *   4. checkCertificationExpiry       → vérification certifications expirées
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProfileScheduler {

    // ── Repositories & Services métier ────────────────────────────────────────
    private final FreelancerProfileRepository profileRepository;
    private final SkillRepository             skillRepository;
    private final CertificationRepository     certificationRepository;
    private final ISkillAuthenticityService   skillAuthenticityService;
    private final ICompletenessService        completenessService;
    private final IEmailService               emailService;
    private final UserClient                  userClient;

    // ── Service de configuration dynamique des schedulers ─────────────────────
    private final ISchedulerConfigService     schedulerConfigService;

    // =========================================================================
    // META-SCHEDULER — tick toutes les 60 secondes
    // =========================================================================

    /**
     * Point d'entrée unique du scheduler dynamique.
     * Exécuté toutes les 60 secondes (fixedDelay = 60 000 ms).
     *
     * À chaque tick :
     *   1. Récupère toutes les configs actives depuis MySQL (lecture à chaud)
     *   2. Pour chaque job actif, vérifie si l'intervalle est écoulé
     *   3. Si oui → exécute le job et met à jour lastRun en base
     */
    @Scheduled(fixedDelay = 60_000)
    public void checkAndRunScheduledJobs() {
        log.debug("[META-SCHEDULER] Tick — vérification des jobs actifs...");

        // Lecture en temps réel depuis la base (configs modifiables sans redémarrage)
        List<SchedulerConfig> activeConfigs = schedulerConfigService.getAllConfigs()
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                .toList();

        if (activeConfigs.isEmpty()) {
            log.debug("[META-SCHEDULER] Aucun job actif configuré.");
            return;
        }

        LocalDateTime maintenant = LocalDateTime.now();

        for (SchedulerConfig config : activeConfigs) {
            if (isDue(config, maintenant)) {
                log.info("[META-SCHEDULER] Déclenchement du job : '{}'", config.getJobName());
                executeJob(config.getJobName());
                // Mise à jour du lastRun en base après exécution
                schedulerConfigService.markLastRun(config.getJobName());
            }
        }
    }

    /**
     * Détermine si un job doit être exécuté maintenant.
     *
     * Règle : le job est dû si lastRun + intervalMinutes <= maintenant.
     *         Si lastRun est null (jamais exécuté), on attend le premier intervalle
     *         complet depuis le démarrage (évite une rafale au boot).
     *
     * @param config     configuration du job lue depuis MySQL
     * @param maintenant timestamp courant
     * @return true si le job doit s'exécuter
     */
    private boolean isDue(SchedulerConfig config, LocalDateTime maintenant) {
        // Jamais exécuté → on exécute au prochain tick (premier démarrage)
        if (config.getLastRun() == null) {
            return true;
        }
        LocalDateTime prochainRun = config.getLastRun()
                .plusMinutes(config.getIntervalMinutes());
        return !maintenant.isBefore(prochainRun);
    }

    /**
     * Déclenche manuellement un job par son nom (utilisé par le endpoint /run).
     * Appelé depuis SchedulerConfigController pour les tests et la démo jury.
     *
     * @param jobName identifiant du job à exécuter immédiatement
     */
    public void triggerJobManually(String jobName) {
        log.info("[MANUAL TRIGGER] Déclenchement manuel du job : '{}'", jobName);
        executeJob(jobName);
        schedulerConfigService.markLastRun(jobName);
    }

    /**
     * Dispatch vers le bon job selon le jobName stocké en base.
     * Centralise la gestion des erreurs pour ne pas bloquer les autres jobs.
     *
     * @param jobName identifiant du job (correspond au nom de la méthode)
     */
    private void executeJob(String jobName) {
        try {
            switch (jobName) {
                case "recalculateAllSkillScores"      -> recalculateAllSkillScores();
                case "updateRegionalRankings"         -> updateRegionalRankings();
                case "sendProfileCompletionReminders" -> sendProfileCompletionReminders();
                case "checkCertificationExpiry"       -> checkCertificationExpiry();
                default -> log.warn("[META-SCHEDULER] Job inconnu ignoré : '{}'", jobName);
            }
        } catch (Exception e) {
            log.error("[META-SCHEDULER] Erreur lors de l'exécution du job '{}' : {}",
                    jobName, e.getMessage(), e);
        }
    }

    // =========================================================================
    // TÂCHE 1 — Recalcul des scores d'authenticité
    // =========================================================================

    /**
     * Recalcule les scores d'authenticité des compétences pour tous les profils.
     * Défaut : intervalMinutes=1440 (toutes les 24h)
     */
    public void recalculateAllSkillScores() {
        log.info(">>> [JOB] Début recalcul scores authenticité...");

        List<FreelancerProfile> profiles = profileRepository.findAll();
        int count = 0;

        for (FreelancerProfile profile : profiles) {
            try {
                skillAuthenticityService.recalculateAllScores(profile.getId());
                count++;
            } catch (Exception e) {
                log.error("Erreur recalcul profil {} : {}", profile.getId(), e.getMessage());
            }
        }

        log.info(">>> [JOB] Scores recalculés pour {} profils.", count);
    }

    // =========================================================================
    // TÂCHE 2 — Classement régional
    // =========================================================================

    /**
     * Met à jour le classement régional des freelancers par score de complétude.
     * Défaut : intervalMinutes=10080 (toutes les 7 jours)
     */
    public void updateRegionalRankings() {
        log.info(">>> [JOB] Début mise à jour classements régionaux...");

        List<FreelancerProfile> allProfiles = profileRepository.findAll();
        List<String> regions = allProfiles.stream()
                .map(FreelancerProfile::getRegion)
                .filter(r -> r != null && !r.isBlank())
                .distinct()
                .toList();

        for (String region : regions) {
            List<FreelancerProfile> ranked =
                    profileRepository.findByRegionOrderByCompletenessScoreDesc(region);

            for (int i = 0; i < ranked.size(); i++) {
                ranked.get(i).setRegionalRank(i + 1);
            }

            profileRepository.saveAll(ranked);
            log.info(">>> [JOB] Région '{}' : {} freelancers classés.", region, ranked.size());
        }

        log.info(">>> [JOB] Classements régionaux mis à jour.");
    }

    // =========================================================================
    // TÂCHE 3 — Rappels profils incomplets
    // =========================================================================

    /**
     * Envoie des emails de rappel aux freelancers dont le profil est incomplet (< 60%).
     * Défaut : intervalMinutes=1440 (toutes les 24h)
     */
    public void sendProfileCompletionReminders() {
        log.info(">>> [JOB] Début vérification profils incomplets...");

        List<FreelancerProfile> incompleteProfiles =
                profileRepository.findProfilesBelowScore(60);

        int emailSentCount = 0;

        for (FreelancerProfile profile : incompleteProfiles) {
            try {
                // Recalculer le score avant d'envoyer
                completenessService.calculateCompleteness(profile.getUserId());

                log.info(">>> [JOB] Rappel profil userId={} score={}",
                        profile.getUserId(), profile.getCompletenessScore());

                // Récupérer email + nom depuis user-service via Feign
                String email    = userClient.getUserEmail(profile.getUserId());
                String fullName = userClient.getUserFullName(profile.getUserId());

                if (email != null && !email.isBlank()) {
                    emailService.sendProfileIncompleteReminder(
                            email,
                            fullName,
                            profile.getCompletenessScore() != null
                                    ? profile.getCompletenessScore()
                                    : 0
                    );
                    emailSentCount++;
                    log.info(">>> [JOB] Email rappel envoyé → {}", email);
                } else {
                    log.warn(">>> [JOB] Email absent pour userId={}", profile.getUserId());
                }

            } catch (Exception e) {
                log.error("Erreur rappel profil {} : {}", profile.getId(), e.getMessage());
            }
        }

        log.info(">>> [JOB] {} profils incomplets, {} emails envoyés.",
                incompleteProfiles.size(), emailSentCount);
    }

    // =========================================================================
    // TÂCHE 4 — Vérification expiration certifications
    // =========================================================================

    /**
     * Marque les certifications expirées et alerte les freelancers par email.
     * Défaut : intervalMinutes=43200 (toutes les 30 jours)
     */
    public void checkCertificationExpiry() {
        log.info(">>> [JOB] Début vérification expiration certifications...");

        LocalDate today    = LocalDate.now();
        LocalDate deadline = today.plusDays(30);

        List<Certification> expiring =
                certificationRepository.findExpiringCertifications(deadline);

        int expiredCount          = 0;
        int emailSentCount        = 0;
        Set<Long> impactedUserIds = new HashSet<>();

        for (Certification cert : expiring) {
            try {
                Long userId = cert.getProfile().getUserId();

                if (cert.getExpiryDate() != null && cert.getExpiryDate().isBefore(today)) {
                    // ── Certification déjà expirée → marquer + alerter ────
                    cert.setIsExpired(true);
                    certificationRepository.save(cert);
                    expiredCount++;
                    impactedUserIds.add(userId);

                    log.info(">>> [JOB] Certification expirée : '{}' (userId={})",
                            cert.getTitle(), userId);

                    // Envoyer l'email d'alerte même pour les certs déjà expirées
                    String email    = userClient.getUserEmail(userId);
                    String fullName = userClient.getUserFullName(userId);

                    if (email != null && !email.isBlank()) {
                        emailService.sendCertificationExpiryAlert(
                                email,
                                fullName,
                                cert.getTitle(),
                                cert.getExpiryDate().toString()
                        );
                        emailSentCount++;
                        log.info(">>> [JOB] Email alerte expiration (expirée) → {}", email);
                    } else {
                        log.warn(">>> [JOB] Email absent pour userId={}", userId);
                    }

                } else {
                    // ── Certification expire dans < 30 jours → alerter ────
                    log.warn(">>> [JOB] Certification '{}' expire le {} (userId={})",
                            cert.getTitle(), cert.getExpiryDate(), userId);

                    String email    = userClient.getUserEmail(userId);
                    String fullName = userClient.getUserFullName(userId);

                    if (email != null && !email.isBlank()) {
                        emailService.sendCertificationExpiryAlert(
                                email,
                                fullName,
                                cert.getTitle(),
                                cert.getExpiryDate().toString()
                        );
                        emailSentCount++;
                        log.info(">>> [JOB] Email alerte expiration → {}", email);
                    } else {
                        log.warn(">>> [JOB] Email absent pour userId={}", userId);
                    }
                }

            } catch (Exception e) {
                log.error("Erreur traitement certification {} : {}", cert.getId(), e.getMessage());
            }
        }

        // Recalculer la complétude des profils impactés par les expirations
        int recalculatedCount = 0;
        for (Long userId : impactedUserIds) {
            try {
                completenessService.calculateCompleteness(userId);
                recalculatedCount++;
                log.info(">>> [JOB] Completeness recalculé pour userId={}", userId);
            } catch (Exception e) {
                log.error("Erreur recalcul completeness userId={} : {}", userId, e.getMessage());
            }
        }

        log.info(">>> [JOB] {} certifications expirées marquées.", expiredCount);
        log.info(">>> [JOB] {} emails d'alerte envoyés.", emailSentCount);
        log.info(">>> [JOB] Completeness recalculé pour {} profil(s).", recalculatedCount);
    }
}