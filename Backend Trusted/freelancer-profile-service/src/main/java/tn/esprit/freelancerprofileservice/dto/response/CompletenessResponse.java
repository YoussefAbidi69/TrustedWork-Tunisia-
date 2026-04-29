package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO de réponse — score de complétude du profil avec suggestions
 */
@Data
@Builder
public class CompletenessResponse {

    private Integer score;           // Score total sur 100
    private Integer bioScore;        // 0 ou 15
    private Integer avatarScore;     // 0 ou 10
    private Integer skillsScore;     // 0 à 25
    private Integer portfolioScore;  // 0 à 20
    private Integer certifScore;     // 0 ou 15
    private Integer workExpScore;    // 0 ou 15

    // Suggestions personnalisées générées automatiquement
    private List<String> suggestions;
}