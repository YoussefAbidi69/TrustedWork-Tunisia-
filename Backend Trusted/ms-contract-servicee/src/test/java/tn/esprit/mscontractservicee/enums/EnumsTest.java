package tn.esprit.mscontractservicee.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumsTest {

    @Test
    void testContractStatusValues() {
        ContractStatus[] values = ContractStatus.values();
        assertTrue(values.length > 0);
        assertNotNull(ContractStatus.valueOf("DRAFT"));
        assertNotNull(ContractStatus.valueOf("ACTIVE"));
        assertNotNull(ContractStatus.valueOf("COMPLETED"));
        assertNotNull(ContractStatus.valueOf("CANCELLED"));
    }

    @Test
    void testMilestoneStatusValues() {
        MilestoneStatus[] values = MilestoneStatus.values();
        assertTrue(values.length > 0);
        assertNotNull(MilestoneStatus.valueOf("PENDING"));
        assertNotNull(MilestoneStatus.valueOf("SUBMITTED"));
        assertNotNull(MilestoneStatus.valueOf("APPROVED"));
        assertNotNull(MilestoneStatus.valueOf("REJECTED"));
    }

    @Test
    void testDisputeStatusValues() {
        DisputeStatus[] values = DisputeStatus.values();
        assertTrue(values.length > 0);
        assertNotNull(DisputeStatus.valueOf("OPEN"));
        assertNotNull(DisputeStatus.valueOf("RESPONDED"));
        assertNotNull(DisputeStatus.valueOf("UNDER_REVIEW"));
    }

    @Test
    void testNotificationTypeValues() {
        NotificationType[] values = NotificationType.values();
        assertTrue(values.length > 0);
        assertNotNull(NotificationType.valueOf("INFO"));
        assertNotNull(NotificationType.valueOf("WARNING"));
        assertNotNull(NotificationType.valueOf("URGENT"));
    }

    @Test
    void testTransactionTypeValues() {
        TransactionType[] values = TransactionType.values();
        assertTrue(values.length > 0);
        assertNotNull(TransactionType.valueOf("DEPOSIT"));
        assertNotNull(TransactionType.valueOf("REFUND"));
        assertNotNull(TransactionType.valueOf("RELEASE"));
    }

    @Test
    void testEscrowStatusValues() {
        EscrowStatus[] values = EscrowStatus.values();
        assertTrue(values.length > 0);
        assertNotNull(EscrowStatus.valueOf("LOCKED"));
        assertNotNull(EscrowStatus.valueOf("RELEASED"));
        assertNotNull(EscrowStatus.valueOf("DISPUTED"));
    }
}
