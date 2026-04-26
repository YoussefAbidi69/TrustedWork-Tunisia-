package tn.esprit.mscontractservicee.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.entity.*;
import tn.esprit.mscontractservicee.enums.*;
import tn.esprit.mscontractservicee.repository.*;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplExtraTest {

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private MilestoneRepository milestoneRepository;
    @Mock
    private EscrowAccountRepository escrowAccountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private IWalletService walletService;
    @Mock
    private IStripeService stripeService;
    @Mock
    private ContractTotalService contractTotalService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void testCreatePaymentIntent_StripeException_ThrowsBadRequest() throws com.stripe.exception.StripeException {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(new BigDecimal("100"));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        
        when(stripeService.createPaymentIntent(anyLong(), any(), anyString(), anyString()))
                .thenThrow(new com.stripe.exception.StripeException("error", "id", "code", 400) {});

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> paymentService.createPaymentIntent(1L, "test@mail.com"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testAssertContractPayable_SignatureRequiredButNotPendingPayment_Throws() {
        ReflectionTestUtils.setField(paymentService, "signatureRequired", true);
        Contract contract = new Contract();
        contract.setStatus(ContractStatus.ACTIVE);
        
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> ReflectionTestUtils.invokeMethod(paymentService, "assertContractPayable", contract));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testAssertContractPayable_SignatureNotRequiredButInvalidStatus_Throws() {
        ReflectionTestUtils.setField(paymentService, "signatureRequired", false);
        Contract contract = new Contract();
        contract.setStatus(ContractStatus.ACTIVE);
        
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> ReflectionTestUtils.invokeMethod(paymentService, "assertContractPayable", contract));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testConfirmPayment_EscrowAlreadyExists_Throws() throws com.stripe.exception.StripeException {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.PENDING_PAYMENT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(escrowAccountRepository.findByContractId(1L)).thenReturn(Optional.of(new EscrowAccount()));

        com.stripe.model.PaymentIntent mockPI = mock(com.stripe.model.PaymentIntent.class);
        when(mockPI.getStatus()).thenReturn("succeeded");
        when(stripeService.getPaymentIntent(anyString())).thenReturn(mockPI);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> paymentService.confirmPayment("pi_123", 1L));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testReleaseApprovedMilestone_NoContractId_Throws() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(null);
        when(milestoneRepository.findById(10L)).thenReturn(Optional.of(milestone));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> paymentService.releaseApprovedMilestone(10L));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testReleasePaymentToFreelancer_MilestoneMismatch_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(2L);
        when(milestoneRepository.findById(10L)).thenReturn(Optional.of(milestone));

        BigDecimal amount = new BigDecimal("100");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> paymentService.releasePaymentToFreelancer(1L, 10L, amount));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testRequireMilestoneAmount_ZeroAmount_Throws() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setMontant(BigDecimal.ZERO);
        
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> ReflectionTestUtils.invokeMethod(paymentService, "requireMilestoneAmount", milestone, null));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testRequireMilestoneAmount_Mismatch_Throws() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setMontant(new BigDecimal("100"));
        
        BigDecimal amount = new BigDecimal("200");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> ReflectionTestUtils.invokeMethod(paymentService, "requireMilestoneAmount", milestone, amount));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testAssertReleasePreconditions_ContractNotActive_Throws() {
        Contract contract = new Contract();
        contract.setStatus(ContractStatus.DRAFT);
        Milestone milestone = new Milestone();
        
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> ReflectionTestUtils.invokeMethod(paymentService, "assertReleasePreconditions", contract, milestone));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testAssertReleasePreconditions_MilestoneNotApproved_Throws() {
        Contract contract = new Contract();
        contract.setStatus(ContractStatus.ACTIVE);
        Milestone milestone = new Milestone();
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> ReflectionTestUtils.invokeMethod(paymentService, "assertReleasePreconditions", contract, milestone));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testAssertNotAlreadyReleased_Throws() {
        when(transactionRepository.existsByMilestoneIdAndType(10L, TransactionType.RELEASE)).thenReturn(true);
        
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> ReflectionTestUtils.invokeMethod(paymentService, "assertNotAlreadyReleased", 10L));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testAssertEscrowCanRelease_Disputed_Throws() {
        EscrowAccount escrow = new EscrowAccount();
        escrow.setStatus(EscrowStatus.DISPUTED);
        
        BigDecimal amount = new BigDecimal("100");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> ReflectionTestUtils.invokeMethod(paymentService, "assertEscrowCanRelease", escrow, amount));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testAssertEscrowCanRelease_InsufficientBalance_Throws() {
        EscrowAccount escrow = new EscrowAccount();
        escrow.setStatus(EscrowStatus.LOCKED);
        escrow.setMontantBloque(new BigDecimal("50"));
        
        BigDecimal amount = new BigDecimal("100");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> ReflectionTestUtils.invokeMethod(paymentService, "assertEscrowCanRelease", escrow, amount));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testComputeReleaseAmounts_NegativeNet_Throws() {
        Contract contract = new Contract();
        contract.setCommissionRate(new BigDecimal("110"));
        
        BigDecimal amount = new BigDecimal("100");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> ReflectionTestUtils.invokeMethod(paymentService, "computeReleaseAmounts", contract, amount));
        assertEquals(400, ex.getStatusCode().value());
    }
}
