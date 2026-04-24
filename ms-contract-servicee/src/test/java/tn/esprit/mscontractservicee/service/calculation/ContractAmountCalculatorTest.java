package tn.esprit.mscontractservicee.service.calculation;

import org.junit.jupiter.api.Test;
import tn.esprit.mscontractservicee.entity.Milestone;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContractAmountCalculatorTest {

    @Test
    void computeMilestonesTotal_empty_isZero() {
        assertEquals(0, ContractAmountCalculator.computeMilestonesTotal(List.of()).compareTo(BigDecimal.ZERO));
    }

    @Test
    void computeMilestonesTotal_sumsAmounts() {
        Milestone m1 = Milestone.builder().id(1L).montant(new BigDecimal("200.50")).build();
        Milestone m2 = Milestone.builder().id(2L).montant(new BigDecimal("10.00")).build();
        BigDecimal total = ContractAmountCalculator.computeMilestonesTotal(List.of(m1, m2));
        assertEquals(0, total.compareTo(new BigDecimal("210.50")));
    }

    @Test
    void computeMilestonesTotal_nullAmount_throws() {
        Milestone m1 = Milestone.builder().id(1L).montant(null).build();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ContractAmountCalculator.computeMilestonesTotal(List.of(m1)));
        assertTrue(ex.getMessage().contains("Milestone montant is required"));
    }

    @Test
    void computeMilestonesTotal_nonPositive_throws() {
        Milestone m1 = Milestone.builder().id(1L).montant(BigDecimal.ZERO).build();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ContractAmountCalculator.computeMilestonesTotal(List.of(m1)));
        assertTrue(ex.getMessage().contains("must be > 0"));
    }

    @Test
    void amountsMatch_usesCompareTo() {
        assertTrue(ContractAmountCalculator.amountsMatch(new BigDecimal("10.0"), new BigDecimal("10.00")));
        assertFalse(ContractAmountCalculator.amountsMatch(new BigDecimal("10.01"), new BigDecimal("10.00")));
        assertTrue(ContractAmountCalculator.amountsMatch(null, BigDecimal.ZERO));
        assertFalse(ContractAmountCalculator.amountsMatch(null, new BigDecimal("1.00")));
    }
}

