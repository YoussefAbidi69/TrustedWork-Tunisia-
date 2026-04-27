package tn.esprit.msprojectservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.msprojectservice.dto.BurndownChartDTO;
import tn.esprit.msprojectservice.services.IBurndownService;

/**
 * Controller REST — Burndown Chart
 * Module 08 TrustedWork Tunisia
 *
 * Expose les données du graphique burndown pour l'affichage Angular.
 * Base URL : /api/projects
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Burndown Chart", description = "Données du graphique burndown pour le suivi d'avancement du projet")
public class BurndownRestController {

    private final IBurndownService burndownService;

    /**
     * Retourne toutes les données nécessaires pour tracer le burndown chart d'un projet.
     *
     * Réponse :
     *   - courbeIdeale  : liste de points { date, tachesRestantes } en décroissance linéaire
     *   - courbeReelle  : liste de points { date, tachesRestantes } basée sur l'historique réel
     *                     + projection linéaire pour les dates futures
     *   - métriques     : velociteJournaliere, retardEstimeJours, dateLivraisonProjetee, statutBurndown
     *   - analyse       : message explicatif en français généré automatiquement
     *
     * Codes HTTP :
     *   200 OK          : données retournées avec succès
     *   404 NOT FOUND   : projet introuvable
     *   400 BAD REQUEST : projet sans startDate ou endDate
     */
    @GetMapping("/{projectId}/burndown")
    @Operation(
            summary     = "Burndown Chart d'un projet",
            description = "Retourne la courbe idéale, la courbe réelle et les métriques d'avancement " +
                    "(vélocité, retard estimé, date de livraison projetée) pour affichage dans Angular. " +
                    "La courbe réelle est reconstituée depuis l'historique des tâches DONE " +
                    "et projetée par régression linéaire pour les dates futures."
    )
    public ResponseEntity<BurndownChartDTO> getBurndownChart(@PathVariable Long projectId) {
        BurndownChartDTO chart = burndownService.getBurndownChart(projectId);
        return ResponseEntity.ok(chart);
    }
}