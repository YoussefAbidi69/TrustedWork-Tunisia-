package tn.esprit.smartjobboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Read-only success probability, confidence tier, and explainability fields for dashboards and gauges.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuccessPredictionViewDto {
    private double probability;
    private String confidenceLabel;
    /** 0–100 overlap between freelancer skills and merged job skills */
    private double skillOverlapPercent;
    /** 0–100 platform reputation proxy */
    private double reputationScore;
    /** 0–100 historical success proxy */
    private double successRateScore;
    /** Human-readable summary for teachers / users */
    private String predictionSummary;
}
