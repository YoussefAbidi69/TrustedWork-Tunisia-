package tn.esprit.userservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.userservice.entity.AccountStatus;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.repository.UserRepository;
import tn.esprit.userservice.service.ITrustLevelService;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserScheduler {

    private final UserRepository userRepository;
    private final ITrustLevelService trustLevelService;

    // ==================== TÂCHE 1 ====================
    /**
     * Déverrouillage automatique des comptes temporairement lockés.
     * Exécution : toutes les 5 minutes.
     * Logique : si lockedUntil < maintenant → déverrouiller.
     */
    @Scheduled(fixedRate = 300000)
    public void unlockExpiredAccounts() {
        List<User> lockedUsers = userRepository.findByAccountNonLockedFalse();

        int count = 0;
        for (User user : lockedUsers) {
            // Ne déverrouiller que les locks temporaires (lockedUntil défini)
            // Les suspensions admin (lockedUntil = null) ne sont pas touchées
            if (user.getLockedUntil() != null
                    && user.getLockedUntil().isBefore(LocalDateTime.now())) {

                user.setAccountNonLocked(true);
                user.setFailedAttempts(0);
                user.setLockedUntil(null);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
                count++;

                log.info("Compte déverrouillé automatiquement : {}", user.getEmail());
            }
        }

        if (count > 0) {
            log.info("Déverrouillage automatique : {} compte(s) déverrouillé(s)", count);
        }
    }

    // ==================== TÂCHE 2 ====================
    /**
     * Recalcul périodique du Trust Level pour tous les utilisateurs actifs.
     * Exécution : tous les jours à 3h00.
     * Utile car l'Account Age Score évolue avec le temps.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void recomputeTrustLevels() {
        log.info("Début du recalcul périodique des Trust Levels — {}",
                LocalDateTime.now());

        trustLevelService.recomputeAll();

        log.info("Recalcul des Trust Levels terminé — {}",
                LocalDateTime.now());
    }

    // ==================== TÂCHE 3 ====================
    /**
     * Rapport quotidien des utilisateurs supprimés (soft delete).
     * Exécution : tous les jours à 2h00.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void logDeletedUsersCount() {
        List<User> deletedUsers = userRepository
                .findByAccountStatus(AccountStatus.DELETED);

        log.info("Rapport quotidien — Utilisateurs soft-deleted : {} — {}",
                deletedUsers.size(), LocalDateTime.now());
    }

    // ==================== TÂCHE 4 ====================
    /**
     * Rapport quotidien des comptes suspendus actifs.
     * Exécution : tous les jours à 2h30.
     */
    @Scheduled(cron = "0 30 2 * * *")
    public void logSuspendedUsersCount() {
        List<User> suspendedUsers = userRepository
                .findByAccountStatus(AccountStatus.SUSPENDED);

        log.info("Rapport quotidien — Comptes suspendus actifs : {} — {}",
                suspendedUsers.size(), LocalDateTime.now());
    }
}