package tn.esprit.mscontractservicee.dto.dispute;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO stateless retourné par l'analyse AI du litige.
 * Aucune persistance – réponse à la volée uniquement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeAiRecommendation {

    /** Décision suggérée : RESOLVED_CLIENT | RESOLVED_FREELANCER | SPLIT | DISMISSED */
    private String suggestedDecision;

    /** Score de confiance entre 0.0 et 1.0 */
    private Double confidenceScore;

    /** Niveau de risque du litige : LOW | MEDIUM | HIGH */
    private String riskLevel;

    /** Explication détaillée du raisonnement de l'IA */
    private String reasoning;

    /** Résumé neutre du litige en 2-3 phrases */
    private String summary;

    /** Montant suggéré à rembourser au client */
    private BigDecimal suggestedMontantRembourse;

    /** Montant suggéré à libérer au freelancer */
    private BigDecimal suggestedMontantLibere;

    /** Facteurs clés détectés par l'IA */
    private List<String> keyFactors;

    /** Timestamp de la génération */
    private LocalDateTime generatedAt;

    /** ID du litige analysé */
    private Long disputeId;

    /** true si la réponse vient du cache/fallback, false si Gemini */
    private boolean fallback;
}
