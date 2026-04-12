package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO de réponse — analyse des gaps de compétences
 */
@Data
@Builder
public class SkillGapResponse {

    private List<String> mySkills;       // Skills actuels du freelancer
    private List<String> topSkills;      // Top 10 skills sur la plateforme
    private List<String> gapSkills;      // Skills manquants à acquérir
    private int gapCount;                // Nombre de gaps détectés
}