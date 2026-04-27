package tn.esprit.mscontractservicee.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.dto.dispute.DisputeAssignRequest;
import tn.esprit.mscontractservicee.dto.dispute.DisputeCreateRequest;
import tn.esprit.mscontractservicee.dto.dispute.DisputeResolveRequest;
import tn.esprit.mscontractservicee.dto.dispute.DisputeRespondRequest;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Dispute;
import tn.esprit.mscontractservicee.entity.EscrowAccount;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.entity.Transaction;
import tn.esprit.mscontractservicee.entity.Wallet;
import tn.esprit.mscontractservicee.enums.EscrowStatus;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.enums.DisputeStatus;
import tn.esprit.mscontractservicee.enums.MilestoneStatus;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.DisputeRepository;
import tn.esprit.mscontractservicee.repository.EscrowAccountRepository;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;
import tn.esprit.mscontractservicee.repository.TransactionRepository;
import tn.esprit.mscontractservicee.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DisputeServiceImplTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private EscrowAccountRepository escrowAccountRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private IWalletService walletService;

    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private DisputeServiceImpl disputeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testOpenDispute_Success() {
        Long authenticatedCin = 123L;
        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setContractId(10L);
        req.setMotif("Client unresponsive");

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        contract.setStatus(ContractStatus.ACTIVE);

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(5L);
        escrow.setContractId(10L);

        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
        when(disputeRepository.existsByContractIdAndStatusIn(eq(10L), anySet())).thenReturn(false);
        when(disputeRepository.existsByContractIdAndMilestoneIdIsNullAndStatusIn(eq(10L), anySet())).thenReturn(false);
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.of(escrow));
        
        Dispute savedDispute = new Dispute();
        savedDispute.setId(1L);
        savedDispute.setContractId(10L);
        savedDispute.setStatus(DisputeStatus.OPEN);
        when(disputeRepository.save(any(Dispute.class))).thenReturn(savedDispute);

        Dispute result = disputeService.openDispute(authenticatedCin, req);

        assertNotNull(result);
        assertEquals(DisputeStatus.OPEN, result.getStatus());
        verify(disputeRepository, times(1)).save(any(Dispute.class));
        verify(notificationService, times(2)).createNotification(any(), any(), any(), any(), any());
    }

    @Test
    void testOpenDispute_ContractNotActive_Throws() {
        Long authenticatedCin = 123L;
        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setContractId(10L);
        req.setMotif("Motif");

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setStatus(ContractStatus.DRAFT); // Not ACTIVE

        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        assertThrows(ResponseStatusException.class, () -> disputeService.openDispute(authenticatedCin, req));
    }

    @Test
    void testOpenDispute_DisputeAlreadyExists_Throws() {
        Long authenticatedCin = 123L;
        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setContractId(10L);
        req.setMotif("Motif");

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setStatus(ContractStatus.ACTIVE);

        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
        when(disputeRepository.existsByContractIdAndStatusIn(eq(10L), anySet())).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> disputeService.openDispute(authenticatedCin, req));
    }

    @Test
    void testOpenDispute_MilestoneNotPartOfContract_Throws() {
        Long authenticatedCin = 123L;
        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setContractId(10L);
        req.setMilestoneId(5L);
        req.setMotif("Motif");

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setStatus(ContractStatus.ACTIVE);

        Milestone milestone = new Milestone();
        milestone.setId(5L);
        milestone.setContractId(99L); // Wrong contract

        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
        when(milestoneRepository.findById(5L)).thenReturn(Optional.of(milestone));

        assertThrows(ResponseStatusException.class, () -> disputeService.openDispute(authenticatedCin, req));
    }

    @Test
    void testOpenDispute_NotParticipant() {
        Long authenticatedCin = 999L; // Intrus
        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setContractId(10L);
        req.setMotif("Motif");

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        contract.setStatus(ContractStatus.ACTIVE);

        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                disputeService.openDispute(authenticatedCin, req);
            }
        });
    }

    @Test
    void testAssign_SetsUnderReview() {
        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setStatus(DisputeStatus.OPEN);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> i.getArgument(0));

        DisputeAssignRequest req = new DisputeAssignRequest();
        req.setArbitreId(777L);

        Dispute res = disputeService.assign(1L, 999L, req);
        assertEquals(DisputeStatus.UNDER_REVIEW, res.getStatus());
        assertEquals(777L, res.getArbitreId());
        assertNotNull(res.getAssignedAt());
    }

    @Test
    void testAssign_Unauthorized_Throws() {
        assertThrows(ResponseStatusException.class, () -> disputeService.assign(1L, null, null));
    }

    @Test
    void testAssign_InvalidStatus_Throws() {
        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setStatus(DisputeStatus.UNDER_REVIEW); // Already assigned
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        assertThrows(ResponseStatusException.class, () -> disputeService.assign(1L, 999L, null));
    }

    @Test
    void testAssign_NullRequest_UsesAdminCinAsArbitre() {
        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setStatus(DisputeStatus.OPEN);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> i.getArgument(0));

        Dispute res = disputeService.assign(1L, 999L, null);
        assertEquals(DisputeStatus.UNDER_REVIEW, res.getStatus());
        assertEquals(999L, res.getArbitreId());
        assertNotNull(res.getAssignedAt());
    }

    @Test
    void testRespond_ByDefendant_SetsRespondedAndNotifies() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        dispute.setStatus(DisputeStatus.OPEN);
        dispute.setPlaignantId(123L);
        dispute.setDefendantId(456L);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> i.getArgument(0));

        DisputeRespondRequest req = new DisputeRespondRequest();
        req.setPreuvesDefense("defense");

        Dispute res = disputeService.respond(1L, 456L, false, req);
        assertEquals(DisputeStatus.RESPONDED, res.getStatus());
        verify(notificationService, times(1)).createNotification(eq(123L), any(), any(), any(), any());
    }

    @Test
    void testRespond_UnauthorizedWhenNoAuth() {
        DisputeRespondRequest req = new DisputeRespondRequest();
        req.setPreuvesDefense("proof");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                disputeService.respond(1L, null, false, req);
            }
        });
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void testRespond_BadRequestWhenProofMissing() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                disputeService.respond(1L, 456L, false, new DisputeRespondRequest());
            }
        });
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testRespond_BadRequestWhenDisputeAlreadyClosed() {
        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        dispute.setStatus(DisputeStatus.RESOLVED_CLIENT);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        DisputeRespondRequest req = new DisputeRespondRequest();
        req.setPreuvesDefense("proof");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                disputeService.respond(1L, 456L, true, req);
            }
        });
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testRespond_ForbiddenForNonDefendantWhenNotAdmin() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        dispute.setStatus(DisputeStatus.OPEN);
        dispute.setPlaignantId(123L);
        dispute.setDefendantId(456L);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        DisputeRespondRequest req = new DisputeRespondRequest();
        req.setPreuvesDefense("proof");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                disputeService.respond(1L, 123L, false, req);
            }
        });
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void testGetByIdForUser_ForbiddenWhenNotParticipant() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                disputeService.getByIdForUser(1L, 999L, false);
            }
        });
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void testListByContractForUser_AdminDelegates() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
        when(disputeRepository.findByContractIdOrderByOpenedAtDesc(10L)).thenReturn(List.of(new Dispute()));

        List<Dispute> res = disputeService.listByContractForUser(10L, 1L, true);
        assertEquals(1, res.size());
    }

    @Test
    void testListByContractForUser_BadRequestWhenContractIdMissing() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                disputeService.listByContractForUser(null, 1L, true);
            }
        });
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testListByMilestoneForUser_NonAdminRequiresParticipant() {
        Milestone milestone = new Milestone();
        milestone.setId(5L);
        milestone.setContractId(10L);
        when(milestoneRepository.findById(5L)).thenReturn(Optional.of(milestone));

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                disputeService.listByMilestoneForUser(5L, 999L, false);
            }
        });
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void testResolve_Dismissed_UnfreezesEscrowAndSaves() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> i.getArgument(0));

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(5L);
        escrow.setContractId(10L);
        escrow.setStatus(EscrowStatus.DISPUTED);
        escrow.setMontantBloque(new BigDecimal("100"));
        escrow.setMontantLibere(BigDecimal.ZERO);
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.of(escrow));
        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(i -> i.getArgument(0));

        DisputeResolveRequest req = new DisputeResolveRequest();
        req.setStatus(DisputeStatus.DISMISSED);
        req.setDecision("no case");
        req.setMontantLibere(BigDecimal.ZERO);
        req.setMontantRembourse(BigDecimal.ZERO);

        Dispute res = disputeService.resolve(1L, 999L, req);
        assertEquals(DisputeStatus.DISMISSED, res.getStatus());
        assertNotNull(res.getResolvedAt());
        assertTrue(escrow.getStatus() == EscrowStatus.LOCKED || escrow.getStatus() == EscrowStatus.RELEASED || escrow.getStatus() == EscrowStatus.REFUNDED);
        assertNotNull(escrow.getUpdatedAt());
    }

    @Test
    void testResolve_ResolvedFreelancer_ReleasesAndCredits() {
        org.springframework.test.util.ReflectionTestUtils.setField(disputeService, "platformWalletId", 1L);

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setCommissionRate(BigDecimal.valueOf(10));
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> i.getArgument(0));

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(5L);
        escrow.setContractId(10L);
        escrow.setStatus(EscrowStatus.DISPUTED);
        escrow.setMontantBloque(new BigDecimal("100"));
        escrow.setMontantLibere(BigDecimal.ZERO);
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.of(escrow));
        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(i -> i.getArgument(0));

        Wallet platformWallet = new Wallet();
        platformWallet.setId(1L);
        platformWallet.setUserCin(999L);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(platformWallet));

        Wallet freelancerWallet = new Wallet();
        freelancerWallet.setId(11L);
        when(walletService.getOrCreateWallet(456L)).thenReturn(freelancerWallet);

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        DisputeResolveRequest req = new DisputeResolveRequest();
        req.setStatus(DisputeStatus.RESOLVED_FREELANCER);
        req.setDecision("release");
        req.setMontantRembourse(BigDecimal.ZERO);
        req.setMontantLibere(new BigDecimal("100"));

        Dispute res = disputeService.resolve(1L, 999L, req);
        assertEquals(DisputeStatus.RESOLVED_FREELANCER, res.getStatus());
        verify(walletService).credit(eq(456L), any(BigDecimal.class), anyString());
        verify(walletService).credit(eq(999L), any(BigDecimal.class), anyString());
    }

    @Test
    void testResolve_ResolvedClient_RefundsAndCreditsClient() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> i.getArgument(0));

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(5L);
        escrow.setContractId(10L);
        escrow.setStatus(EscrowStatus.DISPUTED);
        escrow.setMontantBloque(new BigDecimal("100"));
        escrow.setMontantLibere(BigDecimal.ZERO);
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.of(escrow));
        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(i -> i.getArgument(0));

        Wallet clientWallet = new Wallet();
        clientWallet.setId(12L);
        when(walletService.getOrCreateWallet(123L)).thenReturn(clientWallet);

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        DisputeResolveRequest req = new DisputeResolveRequest();
        req.setStatus(DisputeStatus.RESOLVED_CLIENT);
        req.setDecision("refund");
        req.setMontantRembourse(new BigDecimal("100"));
        req.setMontantLibere(BigDecimal.ZERO);

        Dispute res = disputeService.resolve(1L, 999L, req);
        assertEquals(DisputeStatus.RESOLVED_CLIENT, res.getStatus());
        verify(walletService).credit(eq(123L), eq(new BigDecimal("100")), anyString());
    }

    @Test
    void testResolve_Split_Partial_PartiallyReleased_DoesNotCloseContract() {
        org.springframework.test.util.ReflectionTestUtils.setField(disputeService, "platformWalletId", 1L);

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setCommissionRate(BigDecimal.valueOf(10));
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        dispute.setMilestoneId(5L);
        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> i.getArgument(0));

        Milestone milestone = new Milestone();
        milestone.setId(5L);
        milestone.setContractId(10L);
        milestone.setMontant(new BigDecimal("100"));
        when(milestoneRepository.findById(5L)).thenReturn(Optional.of(milestone));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(5L);
        escrow.setContractId(10L);
        escrow.setStatus(EscrowStatus.DISPUTED);
        escrow.setMontantBloque(new BigDecimal("100"));
        escrow.setMontantLibere(BigDecimal.ZERO);
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.of(escrow));
        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(i -> i.getArgument(0));

        Wallet platformWallet = new Wallet();
        platformWallet.setId(1L);
        platformWallet.setUserCin(999L);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(platformWallet));

        when(walletService.getOrCreateWallet(anyLong())).thenAnswer(inv -> {
            Wallet w = new Wallet();
            w.setId(inv.getArgument(0, Long.class)); // stable-ish id per cin
            return w;
        });
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        DisputeResolveRequest req = new DisputeResolveRequest();
        req.setStatus(DisputeStatus.SPLIT);
        req.setDecision("split");
        req.setMontantRembourse(new BigDecimal("20"));
        req.setMontantLibere(new BigDecimal("30"));

        Dispute res = disputeService.resolve(1L, 999L, req);
        assertEquals(DisputeStatus.SPLIT, res.getStatus());
        assertEquals(EscrowStatus.PARTIALLY_RELEASED, escrow.getStatus());
        verify(contractRepository, never()).save(any(Contract.class));
        verify(notificationService, times(2)).createNotification(anyLong(), any(), any(), any(), any());
    }

    @Test
    void testResolve_Split_OneAmountZero_ThrowsBadRequest() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(5L);
        escrow.setContractId(10L);
        escrow.setMontantBloque(new BigDecimal("100"));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.of(escrow));

        DisputeResolveRequest req = new DisputeResolveRequest();
        req.setStatus(DisputeStatus.SPLIT);
        req.setDecision("split");
        req.setMontantRembourse(new BigDecimal("100"));
        req.setMontantLibere(BigDecimal.ZERO); // Error for SPLIT

        assertThrows(ResponseStatusException.class, () -> disputeService.resolve(1L, 999L, req));
    }

    @Test
    void testResolve_SumExceedsEscrow_ThrowsBadRequest() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(5L);
        escrow.setContractId(10L);
        escrow.setMontantBloque(new BigDecimal("100"));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.of(escrow));

        DisputeResolveRequest req = new DisputeResolveRequest();
        req.setStatus(DisputeStatus.SPLIT);
        req.setDecision("split");
        req.setMontantRembourse(new BigDecimal("60"));
        req.setMontantLibere(new BigDecimal("50")); // Total 110 > 100

        assertThrows(ResponseStatusException.class, () -> disputeService.resolve(1L, 999L, req));
    }

    @Test
    void testResolve_InvalidResolutionStatus_Open_ThrowsBadRequest() {
        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(5L);
        escrow.setContractId(10L);
        escrow.setStatus(EscrowStatus.DISPUTED);
        escrow.setMontantBloque(new BigDecimal("100"));
        escrow.setMontantLibere(BigDecimal.ZERO);
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.of(escrow));

        DisputeResolveRequest req = new DisputeResolveRequest();
        req.setStatus(DisputeStatus.OPEN);
        req.setDecision("x");
        req.setMontantRembourse(new BigDecimal("10"));
        req.setMontantLibere(BigDecimal.ZERO);
        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                disputeService.resolve(1L, 999L, req);
            }
        });
    }

    @Test
    void testResolve_Dismissed_MissingDecision_Throws() {
        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        DisputeResolveRequest req = new DisputeResolveRequest();
        req.setStatus(DisputeStatus.DISMISSED);
        req.setDecision(""); // Missing

        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                disputeService.resolve(1L, 999L, req);
            }
        });
    }

    @Test
    void testResolve_SumExceedsMilestone_Throws() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        dispute.setMilestoneId(5L);
        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        Milestone milestone = new Milestone();
        milestone.setId(5L);
        milestone.setMontant(new BigDecimal("100"));
        when(milestoneRepository.findById(5L)).thenReturn(Optional.of(milestone));

        EscrowAccount escrow = new EscrowAccount();
        escrow.setMontantBloque(new BigDecimal("500"));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.of(escrow));

        DisputeResolveRequest req = new DisputeResolveRequest();
        req.setStatus(DisputeStatus.SPLIT);
        req.setMontantRembourse(new BigDecimal("60"));
        req.setMontantLibere(new BigDecimal("50")); // Total 110 > 100 milestone

        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                disputeService.resolve(1L, 999L, req);
            }
        });
    }

    @Test
    void testResolve_NegativeAmount_Throws() {
        DisputeResolveRequest req = new DisputeResolveRequest();
        req.setStatus(DisputeStatus.RESOLVED_CLIENT);
        req.setMontantRembourse(new BigDecimal("-10"));
        req.setMontantLibere(BigDecimal.ZERO);

        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                disputeService.resolve(1L, 999L, req);
            }
        });
    }

    @Test
    void testOpenDispute_MilestoneDisputeAlreadyExists_Throws() {
        Long authCin = 123L;
        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setContractId(10L);
        req.setMilestoneId(5L);
        req.setMotif("motif");

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        Milestone milestone = new Milestone();
        milestone.setId(5L);
        milestone.setContractId(10L);
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        when(milestoneRepository.findById(5L)).thenReturn(Optional.of(milestone));

        when(disputeRepository.existsByContractIdAndStatusIn(anyLong(), anySet())).thenReturn(false);
        when(disputeRepository.existsByMilestoneIdAndStatusIn(eq(5L), anySet())).thenReturn(true);

        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                disputeService.openDispute(authCin, req);
            }
        });
    }
    @Test
    void testAssignDispute_RejectsNonAdmin() {
        DisputeAssignRequest req = new DisputeAssignRequest();
        req.setArbitreId(1L);
        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                disputeService.assign(1L, 100L, req);
            }
        });
    }
}
