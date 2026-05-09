package tn.esprit.msprojectservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de réponse pour GET /api/projects/{id}/burndown
 *
 * Contient toutes les données nécessaires pour tracer un graphique
 * burndown dans Angular (Chart.js, Recharts, ou ngx-charts).
 *
 * Structure du graphique :
 *   - Axe X       : dates (du startDate au endDate du projet)
 *   - Axe Y       : nombre de tâches restantes
 *   - Courbe 1    : courbe idéale (ligne droite théorique)
 *   - Courbe 2    : courbe réelle (progression effective)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BurndownChartDTO {

    // -------------------------------------------------------
    // Métadonnées du projet
    // -------------------------------------------------------

    /** ID du projet concerné */
    private Long projectId;

    /** Titre du projet */
    private String projectTitle;

    /** Date de début du projet (origine de l'axe X) */
    private LocalDate startDate;

    /** Date de fin prévue du projet (fin de l'axe X) */
    private LocalDate endDate;

    /** Nombre total de tâches au démarrage du projet */
    private int totalTaches;

    // -------------------------------------------------------
    // Courbes
    // -------------------------------------------------------

    /**
     * Courbe idéale : décroissance linéaire parfaite.
     * Point de départ : totalTaches à startDate.
     * Point d'arrivée : 0 tâches à endDate.
     * Générée mathématiquement, indépendante de la réalité.
     */
    private List<BurndownPointDTO> courbeIdeale;

    /**
     * Courbe réelle : tâches effectivement restantes jour par jour.
     * Reconstituée depuis le champ updatedAt des tâches avec status = DONE.
     * Pour les dates futures, la tendance est projetée via régression linéaire.
     */
    private List<BurndownPointDTO> courbeReelle;

    // -------------------------------------------------------
    // Métriques d'analyse
    // -------------------------------------------------------

    /** Nombre de tâches restantes aujourd'hui (non DONE) */
    private int tachesRestantesAujourdhui;

    /** Nombre de tâches complétées (DONE) à ce jour */
    private int tachesCompletees;

    /**
     * Retard estimé en jours par rapport à la courbe idéale.
     * Valeur positive = en retard. Valeur négative = en avance.
     */
    private int retardEstimeJours;

    /**
     * Vélocité journalière réelle : nombre moyen de tâches complétées par jour
     * depuis le démarrage du projet.
     */
    private double velociteJournaliere;

    /**
     * Date de livraison projetée basée sur la vélocité actuelle.
     * Peut être après endDate si le projet est en retard.
     */
    private LocalDate dateLivraisonProjetee;

    /**
     * Statut du burndown par rapport à la courbe idéale.
     * EN_AVANCE | DANS_LES_DELAIS | EN_RETARD | CRITIQUE
     */
    private String statutBurndown;

    /**
     * Message d'analyse généré automatiquement en français.
     * Exemple : "Le projet est en retard de 5 jours. À votre rythme actuel,
     * la livraison est estimée au 15 juillet au lieu du 30 juin."
     */
    private String analyse;
}