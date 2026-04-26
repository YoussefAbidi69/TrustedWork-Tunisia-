package tn.esprit.mscontractservicee.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.DeliveryProof;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.enums.DeliveryStatus;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.enums.MilestoneStatus;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.DeliveryProofRepository;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MilestoneServiceImplTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private DeliveryProofRepository deliveryProofRepository;

    @Mock
    private IPaymentService paymentService;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private MilestoneServiceImpl milestoneService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Contract draftContract(Long id, BigDecimal budget) {
        Contract c = new Contract();
        c.setId(id);
        c.setStatus(ContractStatus.DRAFT);
        c.setMontantTotal(budget);
        c.setClientCin(100L);
        c.setFreelancerCin(200L);
        return c;
    }

    @Test
    void testCreateMilestone_Success() {
        Contract contract = draftContract(1L, new BigDecimal("1000"));
        Milestone milestone = new Milestone();
        milestone.setContractId(1L);
        milestone.setMontant(new BigDecimal("400"));

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(1L)).thenReturn(Collections.emptyList());
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

        Milestone result = milestoneService.createMilestone(milestone);

        assertNotNull(result);
        assertEquals(MilestoneStatus.PENDING, result.getStatus());
        verify(milestoneRepository, times(1)).save(milestone);
    }

    @Test
    void testCreateMilestone_ContractBudgetMissing_Throws() {
        Contract contract = draftContract(1L, new BigDecimal("0"));
        contract.setMontantTotal(null);

        Milestone milestone = new Milestone();
        milestone.setContractId(1L);
        milestone.setMontant(new BigDecimal("100"));

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        assertThrows(ResponseStatusException.class, () -> milestoneService.createMilestone(milestone));
    }

    @Test
    void testCreateMilestone_InvalidExistingMilestoneAmount_Throws() {
        Contract contract = draftContract(1L, new BigDecimal("1000"));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        Milestone bad = new Milestone();
        bad.setId(99L);
        bad.setContractId(1L);
        bad.setMontant(null);
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(1L)).thenReturn(List.of(bad));

        Milestone milestone = new Milestone();
        milestone.setContractId(1L);
        milestone.setMontant(new BigDecimal("100"));

        assertThrows(ResponseStatusException.class, () -> milestoneService.createMilestone(milestone));
    }

    @Test
    void testCreateMilestone_NoContractId_Throws() {
        Milestone milestone = new Milestone();
        milestone.setContractId(null);

        assertThrows(ResponseStatusException.class, () -> milestoneService.createMilestone(milestone));
    }

    @Test
    void testCreateMilestone_ContractNotDraft_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setMontantTotal(new BigDecimal("1000"));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        Milestone milestone = new Milestone();
        milestone.setContractId(1L);
        milestone.setMontant(new BigDecimal("400"));

        assertThrows(ResponseStatusException.class, () -> milestoneService.createMilestone(milestone));
    }

    @Test
    void testCreateMilestone_ExceedsBudget_Throws() {
        Contract contract = draftContract(1L, new BigDecimal("500"));
        Milestone milestone = new Milestone();
        milestone.setContractId(1L);
        milestone.setMontant(new BigDecimal("600")); // dépasse le budget

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(1L)).thenReturn(Collections.emptyList());

        assertThrows(ResponseStatusException.class, () -> milestoneService.createMilestone(milestone));
    }

    @Test
    void testUpdateMilestone_OnlyPending() {
        Milestone existing = new Milestone();
        existing.setId(1L);
        existing.setStatus(MilestoneStatus.SUBMITTED); // not PENDING

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(existing));

        Milestone update = new Milestone();
        assertThrows(ResponseStatusException.class, () -> milestoneService.updateMilestone(1L, update));
    }

    @Test
    void testUpdateMilestone_RecomputesBudgetWithOtherMilestones() {
        Contract contract = draftContract(1L, new BigDecimal("1000"));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        Milestone existing = new Milestone();
        existing.setId(1L);
        existing.setContractId(1L);
        existing.setStatus(MilestoneStatus.PENDING);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(existing));

        Milestone other = new Milestone();
        other.setId(2L);
        other.setContractId(1L);
        other.setMontant(new BigDecimal("500"));
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(1L)).thenReturn(List.of(existing, other));

        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

        Milestone update = new Milestone();
        update.setTitre("New");
        update.setDescription("Desc");
        update.setMontant(new BigDecimal("400"));

        Milestone res = milestoneService.updateMilestone(1L, update);
        assertEquals(new BigDecimal("400"), res.getMontant());
        assertEquals("New", res.getTitre());
    }

    @Test
    void testUpdateMilestone_InvalidOtherMilestoneAmount_Throws() {
        Contract contract = draftContract(1L, new BigDecimal("1000"));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        Milestone existing = new Milestone();
        existing.setId(1L);
        existing.setContractId(1L);
        existing.setStatus(MilestoneStatus.PENDING);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(existing));

        Milestone other = new Milestone();
        other.setId(2L);
        other.setContractId(1L);
        other.setMontant(null);
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(1L)).thenReturn(List.of(existing, other));

        Milestone update = new Milestone();
        update.setMontant(new BigDecimal("400"));

        assertThrows(ResponseStatusException.class, () -> milestoneService.updateMilestone(1L, update));
    }

    @Test
    void testDeleteMilestone_DeletesCorrectly() {
        Milestone existing = new Milestone();
        existing.setId(1L);
        existing.setContractId(1L);

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(existing));
        doNothing().when(milestoneRepository).deleteById(1L);

        assertDoesNotThrow(() -> milestoneService.deleteMilestone(1L));
        verify(milestoneRepository, times(1)).deleteById(1L);
    }

    @Test
    void testSubmitMilestone_SendsNotificationToClient() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);

        Milestone milestone = new Milestone();
        milestone.setId(1L);
        milestone.setContractId(1L);
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

        milestoneService.submitMilestone(1L);

        verify(notificationService, times(1)).createNotification(
                eq(100L), any(), any(), any(), any());
    }

    @Test
    void testSubmitMilestone_MilestoneNotFound_Throws() {
        when(milestoneRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> milestoneService.submitMilestone(1L));
    }

    @Test
    void testSubmitMilestone_InvalidStatus_Throws() {
        Milestone milestone = new Milestone();
        milestone.setId(1L);
        milestone.setStatus(MilestoneStatus.PENDING); // Not IN_PROGRESS or REJECTED
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

        assertThrows(ResponseStatusException.class, () -> milestoneService.submitMilestone(1L));
    }

    @Test
    void testApproveMilestone_SendsNotificationToFreelancer() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);

        Milestone milestone = new Milestone();
        milestone.setId(1L);
        milestone.setContractId(1L);
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setMontant(new BigDecimal("500"));

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(1L)).thenReturn(Collections.emptyList());
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

        milestoneService.approveMilestone(1L, 100L);

        verify(notificationService, times(1)).createNotification(
                eq(200L), any(), any(), any(), any());
    }

    @Test
    void testApproveMilestone_MilestoneNotFound_Throws() {
        when(milestoneRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> milestoneService.approveMilestone(1L, 100L));
    }

    @Test
    void testApproveMilestone_InvalidStatus_Throws() {
        Milestone milestone = new Milestone();
        milestone.setId(1L);
        milestone.setStatus(MilestoneStatus.IN_PROGRESS); // Not SUBMITTED
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

        assertThrows(ResponseStatusException.class, () -> milestoneService.approveMilestone(1L, 100L));
    }

    @Test
    void testStartMilestone_SetsInProgress() {
        Milestone m = new Milestone();
        m.setId(1L);
        m.setStatus(MilestoneStatus.PENDING);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(m));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

        Milestone res = milestoneService.startMilestone(1L);
        assertEquals(MilestoneStatus.IN_PROGRESS, res.getStatus());
        assertNotNull(res.getStartedAt());
    }

    @Test
    void testSubmitMilestone_SetsSubmittedAndNotifiesClient() {
        Milestone m = new Milestone();
        m.setId(1L);
        m.setContractId(10L);
        m.setTitre("M1");
        m.setStatus(MilestoneStatus.IN_PROGRESS);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(m));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(100L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        Milestone res = milestoneService.submitMilestone(1L);
        assertEquals(MilestoneStatus.SUBMITTED, res.getStatus());
        assertNotNull(res.getSubmittedAt());
        verify(notificationService, times(1)).createNotification(eq(100L), any(), any(), any(), any());
    }

    @Test
    void testSubmitMilestoneWithProof_SavesProofAndSubmits() {
        Milestone m = new Milestone();
        m.setId(1L);
        m.setContractId(10L);
        m.setTitre("M1");
        m.setStatus(MilestoneStatus.IN_PROGRESS);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(m));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

        when(deliveryProofRepository.findByMilestoneId(1L)).thenReturn(Optional.empty());
        when(deliveryProofRepository.save(any(DeliveryProof.class))).thenAnswer(i -> i.getArgument(0));

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(100L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        tn.esprit.mscontractservicee.dto.DeliveryProofSubmitRequest proof =
                new tn.esprit.mscontractservicee.dto.DeliveryProofSubmitRequest("f", "demo", "git", "c", "md5");

        Milestone res = milestoneService.submitMilestoneWithProof(1L, proof);
        assertEquals(MilestoneStatus.SUBMITTED, res.getStatus());
        verify(deliveryProofRepository, times(1)).save(any(DeliveryProof.class));
    }

    @Test
    void testApproveMilestone_UpdatesProofAndReleasesPayment() {
        Milestone m = new Milestone();
        m.setId(1L);
        m.setContractId(10L);
        m.setTitre("M1");
        m.setStatus(MilestoneStatus.SUBMITTED);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(m));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryProof proof = new DeliveryProof();
        proof.setMilestoneId(1L);
        proof.setStatus(DeliveryStatus.SUBMITTED);
        when(deliveryProofRepository.findByMilestoneId(1L)).thenReturn(Optional.of(proof));
        when(deliveryProofRepository.save(any(DeliveryProof.class))).thenAnswer(i -> i.getArgument(0));

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setFreelancerCin(200L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        doNothing().when(paymentService).releaseApprovedMilestone(1L);

        Milestone res = milestoneService.approveMilestone(1L, 100L);
        assertEquals(MilestoneStatus.APPROVED, res.getStatus());
        verify(paymentService, times(1)).releaseApprovedMilestone(1L);
        verify(notificationService, times(1)).createNotification(eq(200L), any(), any(), any(), any());
    }

    @Test
    void testRejectMilestone_SetsRejectedAndDeadline() {
        Milestone m = new Milestone();
        m.setId(1L);
        m.setContractId(10L);
        m.setTitre("M1");
        m.setStatus(MilestoneStatus.SUBMITTED);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(m));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));
        when(deliveryProofRepository.findByMilestoneId(1L)).thenReturn(Optional.empty());

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setFreelancerCin(200L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        LocalDate newDeadline = LocalDate.now().plusDays(3);
        Milestone res = milestoneService.rejectMilestone(1L, "needs changes", newDeadline);
        assertEquals(MilestoneStatus.REJECTED, res.getStatus());
        assertEquals(newDeadline, res.getDeadline());
        assertEquals("needs changes", res.getRejectionReason());
    }

    @Test
    void testRejectMilestone_MilestoneNotFound_Throws() {
        when(milestoneRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> milestoneService.rejectMilestone(1L, "reason", null));
    }

    @Test
    void testRejectMilestone_NullReason_Throws() {
        assertThrows(ResponseStatusException.class, () -> milestoneService.rejectMilestone(1L, null, null));
    }

    @Test
    void testRejectMilestone_PastDeadline_Throws() {
        LocalDate past = LocalDate.now().minusDays(1);
        assertThrows(ResponseStatusException.class, () -> milestoneService.rejectMilestone(1L, "reason", past));
    }

    @Test
    void testRejectMilestone_InvalidStatus_Throws() {
        Milestone milestone = new Milestone();
        milestone.setId(1L);
        milestone.setStatus(MilestoneStatus.IN_PROGRESS); // Not SUBMITTED
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

        assertThrows(ResponseStatusException.class, () -> milestoneService.rejectMilestone(1L, "reason", null));
    }

    @Test
    void testUpdateRejectedMilestoneDeadline_OnlyRejected() {
        Milestone m = new Milestone();
        m.setId(1L);
        m.setStatus(MilestoneStatus.REJECTED);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(m));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

        LocalDate newDeadline = LocalDate.now().plusDays(2);
        Milestone res = milestoneService.updateRejectedMilestoneDeadline(1L, newDeadline);
        assertEquals(newDeadline, res.getDeadline());
    }

    @Test
    void testUpdateRejectedMilestoneDeadline_MilestoneNotFound_Throws() {
        when(milestoneRepository.findById(1L)).thenReturn(Optional.empty());
        LocalDate now = LocalDate.now();
        assertThrows(ResponseStatusException.class, () -> milestoneService.updateRejectedMilestoneDeadline(1L, now));
    }

    @Test
    void testUpdateRejectedMilestoneDeadline_NullDeadline_Throws() {
        assertThrows(ResponseStatusException.class, () -> milestoneService.updateRejectedMilestoneDeadline(1L, null));
    }

    @Test
    void testUpdateRejectedMilestoneDeadline_PastDeadline_Throws() {
        LocalDate past = LocalDate.now().minusDays(1);
        assertThrows(ResponseStatusException.class, () -> milestoneService.updateRejectedMilestoneDeadline(1L, past));
    }

    @Test
    void testUpdateRejectedMilestoneDeadline_InvalidStatus_Throws() {
        Milestone milestone = new Milestone();
        milestone.setId(1L);
        milestone.setStatus(MilestoneStatus.PENDING); // Not REJECTED
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

        LocalDate now = LocalDate.now();
        assertThrows(ResponseStatusException.class, () -> milestoneService.updateRejectedMilestoneDeadline(1L, now));
    }

    @Test
    void testFindForClientCin_RequiresCin() {
        assertThrows(ResponseStatusException.class, () -> milestoneService.findForClientCin(null));
    }

    @Test
    void testFindForSignedFreelancerCin_ReturnsRepoList() {
        when(milestoneRepository.findForSignedFreelancer(200L)).thenReturn(List.of(new Milestone()));
        assertEquals(1, milestoneService.findForSignedFreelancerCin(200L).size());
    }

    @Test
    void testFindAll_Delegates() {
        PageRequest pr = PageRequest.of(0, 10);
        Page<Milestone> page = new PageImpl<>(List.of(new Milestone()));
        when(milestoneRepository.findAll(pr)).thenReturn(page);

        Page<Milestone> res = milestoneService.findAll(pr);
        assertEquals(1, res.getContent().size());
    }

    @Test
    void testUpdateStatus_NotFound_Throws() {
        when(milestoneRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                milestoneService.updateStatus(1L, MilestoneStatus.APPROVED);
            }
        });
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void testUpdateStatus_Success() {
        Milestone m = new Milestone();
        m.setId(1L);
        m.setStatus(MilestoneStatus.PENDING);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(m));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

        Milestone res = milestoneService.updateStatus(1L, MilestoneStatus.SUBMITTED);
        assertEquals(MilestoneStatus.SUBMITTED, res.getStatus());
    }

    @Test
    void testSubmitMilestoneWithProof_NullProof_ThrowsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                milestoneService.submitMilestoneWithProof(1L, null);
            }
        });
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testAutoApproveMilestone_SetsAutoApprovedAndReleasesPayment() {
        Milestone m = new Milestone();
        m.setId(1L);
        m.setContractId(10L);
        m.setTitre("M1");
        m.setStatus(MilestoneStatus.SUBMITTED);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(m));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));
        when(deliveryProofRepository.findByMilestoneId(1L)).thenReturn(Optional.empty());
        doNothing().when(paymentService).releaseApprovedMilestone(1L);

        Milestone res = milestoneService.autoApproveMilestone(1L, 999L);
        assertEquals(MilestoneStatus.AUTO_APPROVED, res.getStatus());
        assertNotNull(res.getValidatedAt());
        verify(paymentService).releaseApprovedMilestone(1L);
    }

    @Test
    void testAutoApproveMilestone_InvalidStatus_ThrowsBadRequest() {
        Milestone m = new Milestone();
        m.setId(1L);
        m.setStatus(MilestoneStatus.IN_PROGRESS);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(m));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                milestoneService.autoApproveMilestone(1L, 999L);
            }
        });
        assertEquals(400, ex.getStatusCode().value());
    }
}
