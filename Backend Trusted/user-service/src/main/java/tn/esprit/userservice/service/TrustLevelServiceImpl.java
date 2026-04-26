package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.entity.AccountStatus;
import tn.esprit.userservice.entity.KycStatus;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.exception.UserNotFoundException;
import tn.esprit.userservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustLevelServiceImpl implements ITrustLevelService {

    private final UserRepository userRepository;

    // ==================== ALGORITHME PRINCIPAL ====================

    /**
     * Algorithme Trust Level :
     * Trust Level = (KYC_validated x 50) + (2FA_enabled x 30)
     *             + (Account_age_score x 20)
     *
     * Score total sur 100 points → converti en niveau 1 à 5
     *
     * Niveaux :
     *  1 (0-19)   : compte non vérifié
     *  2 (20-39)  : 2FA uniquement
     *  3 (40-59)  : KYC validé
     *  4 (60-79)  : KYC + ancienneté
     *  5 (80-100) : KYC + 2FA (profil complet)
     */
    @Override
    public int computeAndSave(User user) {

        int score = 0;

        // Critère 1 — KYC validé (50 points)
        if (user.getKycStatus() == KycStatus.APPROVED) {
            score += 50;
        }

        // Critère 2 — 2FA activée (30 points)
        if (user.isTwoFactorEnabled()) {
            score += 30;
        }

        // Critère 3 — Ancienneté du compte (20 points)
        // Score progressif : 0 pt < 7j | 5 pts < 30j | 12 pts < 90j | 20 pts >= 90j
        score += computeAccountAgeScore(user.getCreatedAt());

        // Conversion du score (0-100) en niveau (1-5)
        int level = convertScoreToLevel(score);

        // Sauvegarde
        user.setTrustLevel(level);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Trust Level calculé pour {} : score={} → niveau={}",
                user.getEmail(), score, level);

        return level;
    }

    // ==================== GET ====================

    @Override
    public int getTrustLevel(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable : " + userId));
        return user.getTrustLevel();
    }

    // ==================== RECALCUL GLOBAL (SCHEDULER) ====================

    @Override
    public void recomputeAll() {
        List<User> activeUsers = userRepository.findByAccountStatus(AccountStatus.ACTIVE);

        int count = 0;
        for (User user : activeUsers) {
            computeAndSave(user);
            count++;
        }

        log.info("Recalcul Trust Level terminé — {} utilisateurs mis à jour", count);
    }

    // ==================== HELPERS PRIVÉS ====================

    /**
     * Score d'ancienneté progressif sur 20 points.
     */
    private int computeAccountAgeScore(LocalDateTime createdAt) {
        if (createdAt == null) return 0;

        long days = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());

        if (days >= 90) return 20;
        if (days >= 30) return 12;
        if (days >= 7)  return 5;
        return 0;
    }

    /**
     * Conversion score (0-100) → niveau Trust (1-5).
     *
     * Niveau 1 : 0  - 19  (compte non vérifié)
     * Niveau 2 : 20 - 39  (2FA uniquement)
     * Niveau 3 : 40 - 59  (KYC validé)
     * Niveau 4 : 60 - 79  (KYC + 2FA)
     * Niveau 5 : 80 - 100 (profil complet et vérifié)
     */
    private int convertScoreToLevel(int score) {
        if (score >= 80) return 5;
        if (score >= 60) return 4;
        if (score >= 40) return 3;
        if (score >= 20) return 2;
        return 1;
    }
}