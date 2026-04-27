package tn.esprit.msprojectservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.msprojectservice.dto.ProjectDTO;
import tn.esprit.msprojectservice.dto.TaskSuggestionResponseDTO;
import tn.esprit.msprojectservice.dto.TaskSuggestionResponseDTO.TacheSuggereDTO;
import tn.esprit.msprojectservice.services.IProjectService;
import tn.esprit.msprojectservice.services.TaskSuggestionService;
import tn.esprit.msprojectservice.services.TaskSuggestionService.TaskSuggestionDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoint de suggestion de tâches IA.
 *
 * Appelé par Angular à la première ouverture du Kanban
 * (uniquement si tasks.length === 0).
 *
 * GET /api/projects/{projectId}/suggest-tasks
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "🤖 Task Suggestion IA",
        description = "Suggère une séquence ordonnée de tâches selon le type de projet détecté par le modèle ML")
public class TaskSuggestionController {

    private final TaskSuggestionService suggestionService;
    private final IProjectService       projectService;

    /**
     * Retourne les tâches suggérées par le modèle ML pour un projet donné.
     * Angular affiche ces suggestions dans une modale au premier accès au Kanban.
     * Le freelancer accepte ou ignore chaque suggestion.
     *
     * GET /api/projects/{projectId}/suggest-tasks
     */
    @GetMapping("/{projectId}/suggest-tasks")
    @Operation(
            summary = "Obtenir les tâches suggérées par l'IA",
            description = "Le modèle RandomForest détecte le type du projet via son titre, " +
                    "puis retourne la liste ordonnée des tâches recommandées avec " +
                    "titre, description, estimatedHours et deadline calculée automatiquement."
    )
    public ResponseEntity<TaskSuggestionResponseDTO> suggererTaches(
            @PathVariable Long projectId) {

        ProjectDTO project = projectService.getProjectById(projectId);

        // Date de début pour le calcul des deadlines
        LocalDate startDate = project.getStartDate() != null
                ? project.getStartDate()
                : LocalDate.now();

        List<TaskSuggestionDTO> suggestions =
                suggestionService.suggererTaches(project.getTitle(), startDate);

        List<TacheSuggereDTO> taches = suggestions.stream()
                .map(s -> new TacheSuggereDTO(
                        s.getTitle(),
                        s.getDescription(),
                        s.getOrder(),
                        s.getEstimatedHours(),
                        s.getDeadline()
                ))
                .toList();

        TaskSuggestionResponseDTO response = new TaskSuggestionResponseDTO(
                projectId,
                project.getTitle(),
                suggestions.isEmpty() ? "INCONNU" : suggestions.get(0).getDetectedProjectType(),
                taches
        );

        return ResponseEntity.ok(response);
    }
}