package tn.esprit.mscontractservicee.dto.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContractCreateRequest {
    private Long freelancerCin;
    private Long projectId;
    private String projectTitle;
    private String description;
    private BigDecimal montantTotal;
    private Integer slaFreelancerHeures;
    private Integer slaClientJours;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal commissionRate;
}
