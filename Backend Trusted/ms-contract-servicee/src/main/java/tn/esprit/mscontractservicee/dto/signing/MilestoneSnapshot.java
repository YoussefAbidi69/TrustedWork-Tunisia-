package tn.esprit.mscontractservicee.dto.signing;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class MilestoneSnapshot {
    private Long milestoneId;
    private Integer ordre;
    private String titre;
    private String description;
    private BigDecimal montant;
    private LocalDate deadline;
}

