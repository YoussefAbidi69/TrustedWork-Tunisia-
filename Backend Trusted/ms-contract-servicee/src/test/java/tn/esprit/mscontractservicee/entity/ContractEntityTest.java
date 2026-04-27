package tn.esprit.mscontractservicee.entity;

import org.junit.jupiter.api.Test;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.enums.MilestoneStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ContractEntityTest {

    @Test
    void testContractDefaultValues() {
        Contract contract = new Contract();
        assertNull(contract.getId());
        assertEquals(ContractStatus.DRAFT, contract.getStatus());
        assertNull(contract.getReference());
    }

    @Test
    void testContractSetters() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        contract.setMontantTotal(new BigDecimal("1000.00"));
        contract.setStatus(ContractStatus.DRAFT);
        contract.setReference("CTR-ABCD1234");
        contract.setCreatedAt(LocalDateTime.now());

        assertEquals(1L, contract.getId());
        assertEquals(100L, contract.getClientCin());
        assertEquals(200L, contract.getFreelancerCin());
        assertEquals(0, new BigDecimal("1000.00").compareTo(contract.getMontantTotal()));
        assertEquals(ContractStatus.DRAFT, contract.getStatus());
        assertEquals("CTR-ABCD1234", contract.getReference());
        assertNotNull(contract.getCreatedAt());
    }

    @Test
    void testMilestoneDefaults() {
        Milestone milestone = new Milestone();
        assertNull(milestone.getId());
        assertEquals(MilestoneStatus.PENDING, milestone.getStatus());
        assertNull(milestone.getMontant());
    }

    @Test
    void testMilestoneSetters() {
        Milestone milestone = new Milestone();
        milestone.setId(1L);
        milestone.setContractId(10L);
        milestone.setMontant(new BigDecimal("500"));
        milestone.setTitre("Phase 1");

        assertEquals(1L, milestone.getId());
        assertEquals(10L, milestone.getContractId());
        assertEquals(0, new BigDecimal("500").compareTo(milestone.getMontant()));
        assertEquals("Phase 1", milestone.getTitre());
    }

    @Test
    void testNotificationDefaults() {
        Notification notif = new Notification();
        assertNull(notif.getId());
        assertFalse(notif.isRead());
    }

    @Test
    void testWalletBuilderPattern() {
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userCin(123L)
                .balance(new BigDecimal("200"))
                .totalEarned(BigDecimal.ZERO)
                .totalSpent(BigDecimal.ZERO)
                .totalCommissionPaid(BigDecimal.ZERO)
                .build();

        assertEquals(1L, wallet.getId());
        assertEquals(123L, wallet.getUserCin());
        assertEquals(0, new BigDecimal("200").compareTo(wallet.getBalance()));
    }
}
