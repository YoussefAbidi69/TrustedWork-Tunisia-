package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO de réponse — recommandation de parcours de carrière
 */
@Data
@Builder
public class CareerPathResponse {

    private String detectedPath;         // Ex: "Backend Java"
    private String description;          // Description du parcours
    private List<String> nextSteps;      // Prochaines étapes recommandées
    private List<String> currentSkills;  // Skills déjà acquis sur ce parcours
    private List<String> missingSkills;  // Skills manquants pour ce parcours
}