package tn.esprit.mscontractservicee.dto.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.mscontractservicee.enums.ContractStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractResponse {

    private Long id;
    private String reference;
    private Long clientCin;
    private Long freelancerCin;
    private Long clientWalletCin;
    private Long freelancerWalletCin;
    private Long projectId;
    private String projectTitle;
    private String description;
    private BigDecimal montantTotal;
    private Integer slaFreelancerHeures;
    private Integer slaClientJours;
    private LocalDateTime dateSignature;
    private Integer version;
    private LocalDateTime finalizedAt;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal commissionRate;
    private ContractStatus status;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

