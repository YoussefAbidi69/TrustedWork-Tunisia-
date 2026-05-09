package tn.esprit.msprojectservice.services;

import tn.esprit.msprojectservice.dto.BurndownChartDTO;

/**
 * Interface du service Burndown Chart — Module 08 TrustedWork Tunisia
 *
 * Calcule les données nécessaires pour tracer le graphique burndown
 * d'un projet : courbe idéale, courbe réelle, métriques d'analyse.
 */
public interface IBurndownService {

    /**
     * Calcule et retourne les données complètes du burndown chart pour un projet.
     *
     * @param projectId ID du projet à analyser
     * @return BurndownChartDTO contenant les deux courbes + les métriques
     * @throws tn.esprit.msprojectservice.exceptions.EntityNotFoundException si le projet est introuvable
     * @throws IllegalStateException si le projet n'a pas de startDate ou endDate définis
     */
    BurndownChartDTO getBurndownChart(Long projectId);
}