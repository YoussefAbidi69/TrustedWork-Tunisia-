package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entité de configuration dynamique des schedulers.
 * Chaque ligne représente un job planifiable depuis le backoffice.
 *
 * Champs :
 *  - jobName        : identifiant unique du job (ex: "recalculateAllSkillScores")
 *  - description    : libellé lisible pour l'affichage UI
 *  - cronExpression : expression cron active (ex: "0 0 1 * * *")
 *  - intervalMinutes: équivalent en minutes (affiché dans le formulaire)
 *  - enabled        : activer / désactiver le job sans le supprimer
 *  - lastRun        : timestamp de la dernière exécution (mis à jour automatiquement)
 *  - updatedAt      : timestamp de la dernière modification de config
 */
@Entity
@Table(name = "scheduler_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom unique du job — correspond au nom de la méthode dans ProfileScheduler */
    @Column(nullable = false, unique = true, length = 100)
    private String jobName;

    /** Description lisible affichée dans le backoffice */
    @Column(nullable = false, length = 255)
    private String description;

    /**
     * Expression cron active.
     * Exemples :
     *   "0 0 1 * * *"   → chaque nuit à 1h00
     *   "0 0 0 * * MON" → chaque lundi à minuit
     *   "0 0 9 * * *"   → chaque matin à 9h
     *   "0 0 0 1 * *"   → le 1er de chaque mois
     */
    @Column(nullable = false, length = 50)
    private String cronExpression;

    /**
     * Intervalle équivalent en minutes (pour affichage et calcul simplifié).
     * Utilisé par le meta-scheduler pour décider d'exécuter le job.
     * Exemple : 1440 = toutes les 24h
     */
    @Column(nullable = false)
    private Integer intervalMinutes;

    /** true = job actif, false = job suspendu */
    @Column(nullable = false)
    private Boolean enabled;

    /** Timestamp de la dernière exécution réussie (null si jamais exécuté) */
    @Column
    private LocalDateTime lastRun;

    /** Timestamp de la dernière modification de configuration */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** Initialisation automatique de updatedAt avant persist */
    @PrePersist
    public void prePersist() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Mise à jour automatique de updatedAt avant update */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
