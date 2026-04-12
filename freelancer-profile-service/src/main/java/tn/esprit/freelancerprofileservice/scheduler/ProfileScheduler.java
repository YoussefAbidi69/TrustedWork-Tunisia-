package tn.esprit.freelancerprofileservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.freelancerprofileservice.entities.Certification;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.repositories.CertificationRepository;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.services.ICompletenessService;
import tn.esprit.freelancerprofileservice.services.ISkillAuthenticityService;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler automatique du Module 02 — 4 tâches planifiées
 *
 * Tâche 1 : Recalcul nocturne des scores d'authenticité (chaque nuit à 1h)
 * Tâche 2 : Mise à jour du classement régional (chaque lundi)
 * Tâche 3 : Rappels de complétion de profil (chaque matin à 9h)
 * Tâche 4 : Vérification expiration certifications (1er de chaque mois)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProfileScheduler {

    private final FreelancerProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final CertificationRepository certificationRepository;
    private final ISkillAuthenticityService skillAuthenticityService;
    private final ICompletenessService completenessService;

    /**
     * TÂCHE 1 — Recalcul nocturne des scores d'authenticité
     * Exécution : chaque nuit à 1h00
     * Objectif : maintenir les scores à jour automatiquement
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void recalculateAllSkillScores() {
        log.info(">>> [SCHEDULER] Début recalcul scores authenticité...");

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

        log.info(">>> [SCHEDULER] Scores recalculés pour {} profils.", count);
    }

    /**
     * TÂCHE 2 — Mise à jour du classement régional
     * Exécution : chaque lundi à minuit
     * Objectif : classer les freelancers par région selon leur score de complétude
     */
    @Scheduled(cron = "0 0 0 * * MON")
    public void updateRegionalRankings() {
        log.info(">>> [SCHEDULER] Début mise à jour classements régionaux...");

        // Récupérer toutes les régions distinctes
        List<FreelancerProfile> allProfiles = profileRepository.findAll();
        List<String> regions = allProfiles.stream()
                .map(FreelancerProfile::getRegion)
                .filter(r -> r != null && !r.isBlank())
                .distinct()
                .toList();

        for (String region : regions) {
            // Récupérer les profils de cette région triés par score décroissant
            List<FreelancerProfile> ranked =
                    profileRepository.findByRegionOrderByCompletenessScoreDesc(region);

            // Attribuer le rang
            for (int i = 0; i < ranked.size(); i++) {
                ranked.get(i).setRegionalRank(i + 1);
            }
            profileRepository.saveAll(ranked);
            log.info(">>> [SCHEDULER] Région '{}' : {} freelancers classés.", region, ranked.size());
        }

        log.info(">>> [SCHEDULER] Classements régionaux mis à jour.");
    }

    /**
     * TÂCHE 3 — Rappels de complétion de profil
     * Exécution : chaque matin à 9h
     * Objectif : recalculer et logger les profils incomplets (score < 60)
     * En production : envoyer un email via notification-service
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendProfileCompletionReminders() {
        log.info(">>> [SCHEDULER] Début vérification profils incomplets...");

        // Profils avec score inférieur à 60%
        List<FreelancerProfile> incompleteProfiles =
                profileRepository.findProfilesBelowScore(60);

        for (FreelancerProfile profile : incompleteProfiles) {
            try {
                // Recalcul du score pour avoir des suggestions à jour
                completenessService.calculateCompleteness(profile.getUserId());
                log.info(">>> [SCHEDULER] Rappel profil userId={} score={}",
                        profile.getUserId(), profile.getCompletenessScore());
                // TODO : appel notification-service pour envoyer email
            } catch (Exception e) {
                log.error("Erreur rappel profil {} : {}", profile.getId(), e.getMessage());
            }
        }

        log.info(">>> [SCHEDULER] {} profils incomplets détectés.", incompleteProfiles.size());
    }

    /**
     * TÂCHE 4 — Vérification expiration des certifications
     * Exécution : le 1er de chaque mois à minuit
     * Objectif : marquer les certifications expirées automatiquement
     * En production : notifier le freelancer par email
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void checkCertificationExpiry() {
        log.info(">>> [SCHEDULER] Début vérification expiration certifications...");

        // Certifications qui expirent dans les 30 prochains jours
        LocalDate deadline = LocalDate.now().plusDays(30);
        List<Certification> expiring =
                certificationRepository.findExpiringCertifications(deadline);

        int expiredCount = 0;
        for (Certification cert : expiring) {
            if (cert.getExpiryDate().isBefore(LocalDate.now())) {
                // Déjà expirée — marquer comme expirée
                cert.setIsExpired(true);
                certificationRepository.save(cert);
                expiredCount++;
                log.info(">>> [SCHEDULER] Certification expirée : '{}' (profil userId={})",
                        cert.getTitle(), cert.getProfile().getUserId());
            } else {
                // Expire bientôt — logger l'alerte
                log.warn(">>> [SCHEDULER] Certification '{}' expire le {} (profil userId={})",
                        cert.getTitle(), cert.getExpiryDate(), cert.getProfile().getUserId());
                // TODO : appel notification-service pour alerter le freelancer
            }
        }

        log.info(">>> [SCHEDULER] {} certifications expirées marquées.", expiredCount);
    }
}