package tn.esprit.mscontractservicee.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.dto.PaymentIntentResponse;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.EscrowAccount;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.entity.Transaction;
import tn.esprit.mscontractservicee.entity.Wallet;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.enums.EscrowStatus;
import tn.esprit.mscontractservicee.enums.MilestoneStatus;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.EscrowAccountRepository;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private EscrowAccountRepository escrowAccountRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private IStripeService stripeService;

    @Mock
    private IWalletService walletService;

    @Mock
    private ContractTotalService contractTotalService;

    @Mock
    private tn.esprit.mscontractservicee.repository.TransactionRepository transactionRepository;

    @Mock
    private tn.esprit.mscontractservicee.repository.WalletRepository walletRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void testCreatePaymentIntent_Success() throws com.stripe.exception.StripeException {
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "platformWalletId", 1L);

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setStatus(ContractStatus.PENDING_PAYMENT);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        doNothing().when(contractTotalService).assertStoredTotalMatchesMilestones(any(Contract.class));

        com.stripe.model.PaymentIntent pi = new com.stripe.model.PaymentIntent();
        pi.setId("pi_123");
        pi.setClientSecret("secret_123");

        when(stripeService.createPaymentIntent(1L, new BigDecimal("1000"), "usd", "test@mail.com"))
                .thenReturn(pi);

        PaymentIntentResponse response = paymentService.createPaymentIntent(1L, "test@mail.com");

        assertNotNull(response);
        assertEquals("pi_123", response.getPaymentIntentId());
        assertEquals("secret_123", response.getClientSecret());
    }

    @Test
    void testCreatePaymentIntent_StripeException_ThrowsBadRequest() throws Exception {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setStatus(ContractStatus.PENDING_PAYMENT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        doNothing().when(contractTotalService).assertStoredTotalMatchesMilestones(any(Contract.class));

        when(stripeService.createPaymentIntent(anyLong(), any(), anyString(), anyString()))
                .thenThrow(new com.stripe.exception.ApiException("stripe down", null, null, 0, null));

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                paymentService.createPaymentIntent(1L, "test@mail.com"));
    }

    @Test
    void testCreatePaymentIntent_ContractNotPayable_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        doNothing().when(contractTotalService).assertStoredTotalMatchesMilestones(any(Contract.class));

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                paymentService.createPaymentIntent(1L, "test@mail.com"));
    }

    @Test
    void testConfirmPayment_Success() throws com.stripe.exception.StripeException {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(new BigDecimal("1000"));

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        com.stripe.model.PaymentIntent pi = new com.stripe.model.PaymentIntent();
        pi.setStatus("succeeded");
        when(stripeService.getPaymentIntent("pi_123")).thenReturn(pi);
        doNothing().when(contractTotalService).assertStoredTotalMatchesMilestones(any(Contract.class));
        
        tn.esprit.mscontractservicee.entity.Wallet w = new tn.esprit.mscontractservicee.entity.Wallet();
        w.setId(10L);
        when(walletService.getOrCreateWallet(any())).thenReturn(w);

        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> paymentService.confirmPayment("pi_123", 1L));

        verify(escrowAccountRepository, times(1)).save(any(EscrowAccount.class));
        verify(stripeService, times(1)).getPaymentIntent("pi_123");
    }

    @Test
    void testConfirmPayment_StripeException_Throws() throws Exception {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setStatus(ContractStatus.PENDING_PAYMENT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        when(stripeService.getPaymentIntent("pi_123")).thenThrow(new com.stripe.exception.ApiException("boom", null, null, 0, null));

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> paymentService.confirmPayment("pi_123", 1L));
    }

    @Test
    void testConfirmPayment_EscrowAlreadyExists_Throws() throws Exception {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setStatus(ContractStatus.PENDING_PAYMENT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        com.stripe.model.PaymentIntent pi = new com.stripe.model.PaymentIntent();
        pi.setStatus("succeeded");
        when(stripeService.getPaymentIntent("pi_123")).thenReturn(pi);
        doNothing().when(contractTotalService).assertStoredTotalMatchesMilestones(any(Contract.class));

        when(escrowAccountRepository.findByContractId(1L)).thenReturn(Optional.of(new EscrowAccount()));

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> paymentService.confirmPayment("pi_123", 1L));
    }

    @Test
    void testConfirmPayment_ContractNotFound() {
        // Stripe will not be called because contract is not found
        when(contractRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> paymentService.confirmPayment("pi_123", 1L));
    }

    @Test
    void testReleaseApprovedMilestone_Success() {
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "platformWalletId", 1L);
        
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setFreelancerCin(200L);
        contract.setStatus(ContractStatus.ACTIVE);

        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        milestone.setMontant(new BigDecimal("500"));
        milestone.setStatus(tn.esprit.mscontractservicee.enums.MilestoneStatus.APPROVED);

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(100L);
        escrow.setContractId(1L);
        escrow.setMontantBloque(new BigDecimal("1000"));
        escrow.setMontantLibere(BigDecimal.ZERO);
        escrow.setStatus(EscrowStatus.LOCKED);

        when(milestoneRepository.findById(10L)).thenReturn(Optional.of(milestone));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(escrowAccountRepository.findByContractId(1L)).thenReturn(Optional.of(escrow));
        
        tn.esprit.mscontractservicee.entity.Wallet fw = new tn.esprit.mscontractservicee.entity.Wallet();
        fw.setId(11L);
        when(walletService.getOrCreateWallet(any())).thenReturn(fw);

        tn.esprit.mscontractservicee.entity.Wallet pw = new tn.esprit.mscontractservicee.entity.Wallet();
        pw.setId(1L);
        pw.setUserCin(999L);
        when(walletRepository.findById(any())).thenReturn(Optional.of(pw));

        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> paymentService.releaseApprovedMilestone(10L));

        verify(walletService, times(1)).credit(eq(200L), any(BigDecimal.class), anyString());
    }

    @Test
    void testReleaseApprovedMilestone_EscrowDisputed_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setFreelancerCin(200L);
        contract.setStatus(ContractStatus.ACTIVE);

        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        milestone.setMontant(new BigDecimal("500"));
        milestone.setStatus(MilestoneStatus.APPROVED);

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(100L);
        escrow.setContractId(1L);
        escrow.setMontantBloque(new BigDecimal("1000"));
        escrow.setMontantLibere(BigDecimal.ZERO);
        escrow.setStatus(EscrowStatus.DISPUTED);

        when(milestoneRepository.findById(10L)).thenReturn(Optional.of(milestone));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(transactionRepository.existsByMilestoneIdAndType(10L, tn.esprit.mscontractservicee.enums.TransactionType.RELEASE)).thenReturn(false);
        when(escrowAccountRepository.findByContractId(1L)).thenReturn(Optional.of(escrow));

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> paymentService.releaseApprovedMilestone(10L));
    }

    @Test
    void testReleaseApprovedMilestone_CompletesContractWhenFullyReleased() {
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "platformWalletId", 1L);

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setFreelancerCin(200L);
        contract.setStatus(ContractStatus.ACTIVE);

        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        milestone.setMontant(new BigDecimal("500"));
        milestone.setStatus(MilestoneStatus.APPROVED);

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(100L);
        escrow.setContractId(1L);
        escrow.setMontantBloque(new BigDecimal("500"));
        escrow.setMontantLibere(BigDecimal.ZERO);
        escrow.setMontantTotal(new BigDecimal("500"));
        escrow.setStatus(EscrowStatus.LOCKED);

        when(milestoneRepository.findById(10L)).thenReturn(Optional.of(milestone));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(transactionRepository.existsByMilestoneIdAndType(10L, tn.esprit.mscontractservicee.enums.TransactionType.RELEASE)).thenReturn(false);
        when(escrowAccountRepository.findByContractId(1L)).thenReturn(Optional.of(escrow));

        Wallet freelancerWallet = new Wallet();
        freelancerWallet.setId(11L);
        when(walletService.getOrCreateWallet(200L)).thenReturn(freelancerWallet);

        Wallet pw = new Wallet();
        pw.setId(1L);
        pw.setUserCin(999L);
        when(walletRepository.findById(anyLong())).thenReturn(Optional.of(pw));

        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.releaseApprovedMilestone(10L);

        assertEquals(ContractStatus.COMPLETED, contract.getStatus());
    }

    @Test
    void testCreatePaymentIntent_SimulationEnabled_ReturnsSimulatedIds() {
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "simulationEnabled", true);

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setStatus(ContractStatus.DRAFT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        doNothing().when(contractTotalService).assertStoredTotalMatchesMilestones(any(Contract.class));

        PaymentIntentResponse response = paymentService.createPaymentIntent(1L, "test@mail.com");

        assertNotNull(response.getPaymentIntentId());
        assertTrue(response.getPaymentIntentId().startsWith("sim_"));
        assertNotNull(response.getClientSecret());
        verifyNoInteractions(stripeService);
    }

    @Test
    void testConfirmPayment_FailsWhenStripeNotSucceeded() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "simulationEnabled", false);

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setStatus(ContractStatus.PENDING_PAYMENT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        com.stripe.model.PaymentIntent pi = new com.stripe.model.PaymentIntent();
        pi.setStatus("requires_payment_method");
        when(stripeService.getPaymentIntent("pi_123")).thenReturn(pi);

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> paymentService.confirmPayment("pi_123", 1L));
    }

    @Test
    void testGetPaymentStatus_SimulationEnabled() {
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "simulationEnabled", true);

        assertEquals("succeeded", paymentService.getPaymentStatus("sim_ABC"));
        assertEquals("pending", paymentService.getPaymentStatus("pi_real"));
    }

    @Test
    void testGetPaymentStatus_DelegatesToStripe() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "simulationEnabled", false);
        com.stripe.model.PaymentIntent pi = new com.stripe.model.PaymentIntent();
        pi.setStatus("succeeded");
        when(stripeService.getPaymentIntent("pi_123")).thenReturn(pi);
        assertEquals("succeeded", paymentService.getPaymentStatus("pi_123"));
    }

    @Test
    void testRefundMilestoneToClient_Success() {
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "simulationEnabled", true);

        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        milestone.setMontant(new BigDecimal("100"));
        milestone.setStatus(MilestoneStatus.CANCELLED);
        milestone.setTitre("M1");
        when(milestoneRepository.findById(10L)).thenReturn(Optional.of(milestone));

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(5L);
        escrow.setContractId(1L);
        escrow.setMontantBloque(new BigDecimal("500"));
        escrow.setMontantLibere(BigDecimal.ZERO);
        escrow.setMontantTotal(new BigDecimal("500"));
        escrow.setStatus(EscrowStatus.LOCKED);
        when(escrowAccountRepository.findByContractId(1L)).thenReturn(Optional.of(escrow));
        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(i -> i.getArgument(0));

        Wallet wallet = new Wallet();
        wallet.setId(11L);
        when(walletService.getOrCreateWallet(100L)).thenReturn(wallet);

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> paymentService.refundMilestoneToClient(10L));
        verify(walletService).credit(eq(100L), eq(new BigDecimal("100")), anyString());
    }

    @Test
    void testRefundMilestoneToClient_EscrowBecomesReleased() {
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "simulationEnabled", true);

        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        milestone.setMontant(new BigDecimal("100"));
        milestone.setStatus(MilestoneStatus.CANCELLED);
        milestone.setTitre("M1");
        when(milestoneRepository.findById(10L)).thenReturn(Optional.of(milestone));

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(5L);
        escrow.setContractId(1L);
        escrow.setMontantBloque(new BigDecimal("100"));
        escrow.setMontantLibere(BigDecimal.ZERO);
        escrow.setMontantTotal(new BigDecimal("100"));
        escrow.setStatus(EscrowStatus.LOCKED);
        when(escrowAccountRepository.findByContractId(1L)).thenReturn(Optional.of(escrow));
        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(i -> i.getArgument(0));

        Wallet wallet = new Wallet();
        wallet.setId(11L);
        when(walletService.getOrCreateWallet(100L)).thenReturn(wallet);

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.refundMilestoneToClient(10L);
        assertEquals(EscrowStatus.RELEASED, escrow.getStatus());
    }

    @Test
    void testCreatePaymentIntent_SignatureRequired_RejectsWhenNotPendingPayment() {
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "signatureRequired", true);

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setStatus(ContractStatus.DRAFT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        doNothing().when(contractTotalService).assertStoredTotalMatchesMilestones(any(Contract.class));

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> paymentService.createPaymentIntent(1L, "test@mail.com"));
    }

    @Test
    void testReleaseApprovedMilestone_AutoApproved() {
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "platformWalletId", 1L);

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setFreelancerCin(200L);
        contract.setStatus(ContractStatus.ACTIVE);

        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        milestone.setMontant(new BigDecimal("500"));
        milestone.setStatus(MilestoneStatus.AUTO_APPROVED);

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(100L);
        escrow.setContractId(1L);
        escrow.setMontantBloque(new BigDecimal("1000"));
        escrow.setMontantLibere(BigDecimal.ZERO);
        escrow.setStatus(EscrowStatus.LOCKED);

        when(milestoneRepository.findById(10L)).thenReturn(Optional.of(milestone));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(escrowAccountRepository.findByContractId(1L)).thenReturn(Optional.of(escrow));
        
        Wallet fw = new Wallet();
        fw.setId(11L);
        when(walletService.getOrCreateWallet(200L)).thenReturn(fw);

        Wallet pw = new Wallet();
        pw.setId(1L);
        pw.setUserCin(999L);
        when(walletRepository.findById(anyLong())).thenReturn(Optional.of(pw));

        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> paymentService.releaseApprovedMilestone(10L));
        verify(walletService).credit(eq(200L), any(BigDecimal.class), anyString());
    }

    @Test
    void testReleasePaymentToFreelancer_WrongContractId_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(99L); // Wrong
        when(milestoneRepository.findById(10L)).thenReturn(Optional.of(milestone));

        BigDecimal amount = new BigDecimal("100");
        assertThrows(ResponseStatusException.class, () -> 
                paymentService.releasePaymentToFreelancer(1L, 10L, amount));
    }

    @Test
    void testReleasePaymentToFreelancer_AmountMismatch_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        milestone.setMontant(new BigDecimal("100"));
        when(milestoneRepository.findById(10L)).thenReturn(Optional.of(milestone));

        BigDecimal invalidAmount = new BigDecimal("150");
        assertThrows(ResponseStatusException.class, () -> 
                paymentService.releasePaymentToFreelancer(1L, 10L, invalidAmount));
    }

    @Test
    void testReleaseApprovedMilestone_MilestoneMissingContractId_Throws() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(null);
        when(milestoneRepository.findById(10L)).thenReturn(Optional.of(milestone));

        assertThrows(ResponseStatusException.class, () -> paymentService.releaseApprovedMilestone(10L));
    }

    @Test
    void testReleasePaymentToFreelancer_AutoApprovedButNoInvoice_Throws() {
        Contract c = new Contract();
        c.setId(1L);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(c));

        Milestone m = new Milestone();
        m.setId(1L);
        m.setContractId(1L);
        m.setStatus(MilestoneStatus.AUTO_APPROVED);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(m));

        assertThrows(ResponseStatusException.class, () -> paymentService.releasePaymentToFreelancer(1L, 1L, null));
    }
    @Test
    void testRefundMilestoneToClient_NotFound_Throws() {
        when(milestoneRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> paymentService.refundMilestoneToClient(1L));
    }
    @Test
    void testReleaseApprovedMilestone_NoContractId_Throws() {
        Milestone m = new Milestone();
        m.setId(1L);
        m.setContractId(null);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(m));
        assertThrows(ResponseStatusException.class, () -> paymentService.releaseApprovedMilestone(1L));
    }

    @Test
    void testRequireMilestoneAmount_NullMontant_Throws() {
        Milestone m = new Milestone();
        m.setId(1L);
        m.setMontant(null);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(m));
        assertThrows(ResponseStatusException.class, () -> paymentService.releaseApprovedMilestone(1L));
    }
}
