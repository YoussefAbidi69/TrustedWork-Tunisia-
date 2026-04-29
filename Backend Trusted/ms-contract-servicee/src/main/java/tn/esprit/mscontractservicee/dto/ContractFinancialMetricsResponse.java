package tn.esprit.mscontractservicee.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class ContractFinancialMetricsResponse {
    private Long contractId;
    private Integer milestonesCount;
    private BigDecimal storedMontantTotal;
    private BigDecimal computedMontantTotal;
    private BigDecimal delta;
    private BigDecimal remainingBudget;
    private boolean overBudget;
    private boolean readyToFinalize;
    private boolean mismatch;
}
