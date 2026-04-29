package tn.esprit.msprojectservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.msprojectservice.entities.RiskSeverity;

/**
 * DTO retourné par le modèle Random Forest.
 * Contient la prédiction ML + les features utilisées + la sévérité calculée.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLPredictionDTO {

    // --- Résultat principal ---
    /** Probabilité de retard prédite par le modèle (0.0 à 1.0) */
    private double probabilityLate;

    /** Décision finale : true = en retard prédit, false = à temps prédit */
    private boolean willBeLate;

    /** Sévérité déduite depuis la probabilité */
    private RiskSeverity severity;

    /** Message lisible généré automatiquement */
    private String message;

    /** Feature qui a le plus contribué au risque */
    private String criticalFeature;

    // --- Features envoyées au modèle (traçabilité) ---
    private double daysElapsedRatio;
    private double tasksDoneRatio;
    private int activeRisksCount;
    private double bottleneckDaysAvg;
    private int openDeliverablesCount;
    private double assigneePerfScore;
    private double scopeCreepRatio;
    private double budgetConsumptionRatio;
}