package tn.esprit.freelancerprofileservice.services;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Résultat d'analyse métier d'un avis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAnalysisResult {

    private boolean flagged;

    private String flagReason;
}