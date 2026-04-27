package tn.esprit.mscontractservicee.dto.signing;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ContractSnapshot {
    private Long contractId;
    private Integer contractVersion;
    private String reference;
    private Long clientCin;
    private Long freelancerCin;
    private Long projectId;
    private String projectTitle;
    private String description;
    private BigDecimal montantTotal;
    private BigDecimal commissionRate;
    private Integer slaFreelancerHeures;
    private Integer slaClientJours;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDateTime createdAt;
    private List<MilestoneSnapshot> milestones;
}

