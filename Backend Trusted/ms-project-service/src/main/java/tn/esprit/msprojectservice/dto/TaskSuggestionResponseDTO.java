package tn.esprit.msprojectservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Réponse de l'endpoint GET /api/projects/{id}/suggest-tasks
 *
 * Exemple JSON :
 * {
 *   "projectId": 1,
 *   "projectTitle": "TuniShop E-Commerce",
 *   "detectedType": "E_COMMERCE",
 *   "suggestedTasks": [
 *     {
 *       "title": "Maquette UI/UX",
 *       "description": "Créer les wireframes et maquettes Figma...",
 *       "order": 1,
 *       "estimatedHours": 16,
 *       "deadline": "2026-05-01"
 *     },
 *     ...
 *   ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskSuggestionResponseDTO {

    private Long                  projectId;
    private String                projectTitle;
    private String                detectedType;
    private List<TacheSuggereDTO> suggestedTasks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TacheSuggereDTO {
        private String    title;
        private String    description;
        private int       order;
        private int       estimatedHours;
        private LocalDate deadline;
    }
}