package tn.esprit.mscontractservicee.service.calculation;

import tn.esprit.mscontractservicee.entity.Milestone;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pure calculation helpers for contract/milestone amounts.
 * Keep this class dependency-free to make it easy to unit test.
 */
public final class ContractAmountCalculator {

    private ContractAmountCalculator() {
    }

    public static BigDecimal computeMilestonesTotal(List<Milestone> milestones) {
        if (milestones == null || milestones.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Milestone m : milestones) {
            if (m == null) {
                continue;
            }
            BigDecimal amount = m.getMontant();
            if (amount == null) {
                throw new IllegalArgumentException("Milestone montant is required. milestoneId=" + m.getId());
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Milestone montant must be > 0. milestoneId=" + m.getId()
                        + " montant=" + amount);
            }
            total = total.add(amount);
        }
        return total;
    }

    public static boolean amountsMatch(BigDecimal storedContractTotal, BigDecimal computedFromMilestones) {
        BigDecimal stored = storedContractTotal != null ? storedContractTotal : BigDecimal.ZERO;
        BigDecimal computed = computedFromMilestones != null ? computedFromMilestones : BigDecimal.ZERO;
        return stored.compareTo(computed) == 0;
    }
}

