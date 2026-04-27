package tn.esprit.mscontractservicee.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.dto.ContractFinancialMetricsResponse;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;
import tn.esprit.mscontractservicee.service.calculation.ContractAmountCalculator;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractTotalService {

    private final ContractRepository contractRepository;
    private final MilestoneRepository milestoneRepository;

    public BigDecimal computeMilestonesTotal(Long contractId) {
        List<Milestone> milestones = milestoneRepository.findByContractIdOrderByOrdreAsc(contractId);
        return ContractAmountCalculator.computeMilestonesTotal(milestones);
    }

    public ContractFinancialMetricsResponse getFinancialMetrics(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + contractId));

        List<Milestone> milestones = milestoneRepository.findByContractIdOrderByOrdreAsc(contractId);
        final BigDecimal computed;
        try {
            computed = ContractAmountCalculator.computeMilestonesTotal(milestones);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }

        BigDecimal stored = contract.getMontantTotal();
        BigDecimal storedNormalized = stored != null ? stored : BigDecimal.ZERO;
        BigDecimal delta = computed.subtract(storedNormalized);
        boolean mismatch = !ContractAmountCalculator.amountsMatch(stored, computed);
        BigDecimal remainingBudget = storedNormalized.subtract(computed);
        boolean overBudget = computed.compareTo(storedNormalized) > 0;
        boolean readyToFinalize = stored != null && ContractAmountCalculator.amountsMatch(stored, computed);

        return ContractFinancialMetricsResponse.builder()
                .contractId(contractId)
                .milestonesCount(milestones != null ? milestones.size() : 0)
                .storedMontantTotal(stored)
                .computedMontantTotal(computed)
                .delta(delta)
                .remainingBudget(remainingBudget)
                .overBudget(overBudget)
                .readyToFinalize(readyToFinalize)
                .mismatch(mismatch)
                .build();
    }

    /**
     * Payment safety check: refuse to charge if contract.montantTotal does not match milestones sum.
     */
    public void assertStoredTotalMatchesMilestones(Contract contract) {
        if (contract == null || contract.getId() == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Contract is required");
        }
        if (contract.getMontantTotal() == null || contract.getMontantTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Contract montantTotal (budget) must be set and > 0 before payment. contractId="
                    + contract.getId());
        }
        BigDecimal computed = computeMilestonesTotal(contract.getId());
        BigDecimal stored = contract.getMontantTotal();
        if (!ContractAmountCalculator.amountsMatch(stored, computed)) {
            BigDecimal storedNormalized = stored != null ? stored : BigDecimal.ZERO;
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Contract budget not fully allocated. storedBudget=" + storedNormalized
                    + " milestonesTotal=" + computed + " (sum of milestones). Add/update milestones to match the contract budget.");
        }
    }
}
