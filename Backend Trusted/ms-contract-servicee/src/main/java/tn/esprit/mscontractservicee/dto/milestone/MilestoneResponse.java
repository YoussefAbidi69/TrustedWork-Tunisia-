package tn.esprit.mscontractservicee.dto.milestone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.mscontractservicee.enums.MilestoneStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneResponse {

    private Long id;
    private Long contractId;
    private Integer ordre;
    private String titre;
    private String description;
    private BigDecimal montant;
    private LocalDate deadline;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime validatedAt;
    private MilestoneStatus status;
    private String rejectionReason;
}

