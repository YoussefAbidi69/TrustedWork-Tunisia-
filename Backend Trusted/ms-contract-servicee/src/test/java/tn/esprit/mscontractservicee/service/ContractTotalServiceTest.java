package tn.esprit.mscontractservicee.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.dto.ContractFinancialMetricsResponse;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContractTotalServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @InjectMocks
    private ContractTotalService contractTotalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testComputeMilestonesTotal_NoMilestones() {
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(1L)).thenReturn(Collections.emptyList());

        BigDecimal result = contractTotalService.computeMilestonesTotal(1L);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void testComputeMilestonesTotal_WithMilestones() {
        Milestone m1 = new Milestone(); m1.setMontant(new BigDecimal("400.00"));
        Milestone m2 = new Milestone(); m2.setMontant(new BigDecimal("600.00"));
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(1L)).thenReturn(Arrays.asList(m1, m2));

        BigDecimal result = contractTotalService.computeMilestonesTotal(1L);

        assertEquals(new BigDecimal("1000.00"), result);
    }

    @Test
    void testGetFinancialMetrics_ContractNotFound() {
        when(contractRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                contractTotalService.getFinancialMetrics(99L));
    }

    @Test
    void testGetFinancialMetrics_ReadyToFinalize() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(new BigDecimal("1000.00"));

        Milestone m1 = new Milestone(); m1.setMontant(new BigDecimal("600.00"));
        Milestone m2 = new Milestone(); m2.setMontant(new BigDecimal("400.00"));

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(1L)).thenReturn(Arrays.asList(m1, m2));

        ContractFinancialMetricsResponse metrics = contractTotalService.getFinancialMetrics(1L);

        assertNotNull(metrics);
        assertTrue(metrics.isReadyToFinalize());
        assertFalse(metrics.isMismatch());
        assertFalse(metrics.isOverBudget());
        assertEquals(0, BigDecimal.ZERO.compareTo(metrics.getRemainingBudget()));
    }

    @Test
    void testGetFinancialMetrics_OverBudget() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(new BigDecimal("500.00"));

        Milestone m1 = new Milestone(); m1.setMontant(new BigDecimal("700.00"));

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(1L)).thenReturn(List.of(m1));

        ContractFinancialMetricsResponse metrics = contractTotalService.getFinancialMetrics(1L);

        assertTrue(metrics.isOverBudget());
        assertTrue(metrics.isMismatch());
    }

    @Test
    void testAssertStoredTotalMatchesMilestones_Matching() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(new BigDecimal("1000.00"));

        Milestone m1 = new Milestone(); m1.setMontant(new BigDecimal("1000.00"));
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(1L)).thenReturn(List.of(m1));

        assertDoesNotThrow(() -> contractTotalService.assertStoredTotalMatchesMilestones(contract));
    }

    @Test
    void testAssertStoredTotalMatchesMilestones_Mismatch() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(new BigDecimal("1000.00"));

        Milestone m1 = new Milestone(); m1.setMontant(new BigDecimal("800.00"));
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(1L)).thenReturn(List.of(m1));

        assertThrows(RuntimeException.class, () ->
                contractTotalService.assertStoredTotalMatchesMilestones(contract));
    }

    @Test
    void testAssertStoredTotalMatchesMilestones_NullContract() {
        assertThrows(RuntimeException.class, () ->
                contractTotalService.assertStoredTotalMatchesMilestones(null));
    }

    @Test
    void testAssertStoredTotalMatchesMilestones_NullBudget() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(null);

        assertThrows(RuntimeException.class, () ->
                contractTotalService.assertStoredTotalMatchesMilestones(contract));
    }

    @Test
    void testAssertStoredTotalMatchesMilestones_ZeroBudget_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(BigDecimal.ZERO);
        assertThrows(ResponseStatusException.class, () -> 
            contractTotalService.assertStoredTotalMatchesMilestones(contract));
    }
}
