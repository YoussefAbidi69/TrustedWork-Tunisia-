package tn.esprit.msprojectservice.dto;

import lombok.*;
import tn.esprit.msprojectservice.entities.DeliveryRiskSignal;
import tn.esprit.msprojectservice.entities.RiskType;
import tn.esprit.msprojectservice.entities.RiskSeverity;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRiskSignalDTO {

    private Long id;
    private Long projectId;
    private RiskType riskType;
    private RiskSeverity severity;
    private String message;
    private Long affectedTaskId;
    private boolean resolved;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;

    // --- Mapping Entity -> DTO ---
    public static DeliveryRiskSignalDTO fromEntity(DeliveryRiskSignal signal) {
        return DeliveryRiskSignalDTO.builder()
                .id(signal.getId())
                .projectId(signal.getProject() != null ? signal.getProject().getId() : null)
                .riskType(signal.getRiskType())
                .severity(signal.getSeverity())
                .message(signal.getMessage())
                .affectedTaskId(signal.getAffectedTaskId())
                .resolved(signal.isResolved())
                .detectedAt(signal.getDetectedAt())
                .resolvedAt(signal.getResolvedAt())
                .build();
    }
}