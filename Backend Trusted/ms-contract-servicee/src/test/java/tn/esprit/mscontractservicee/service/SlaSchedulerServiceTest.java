package tn.esprit.mscontractservicee.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.EscrowAccount;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.enums.EscrowStatus;
import tn.esprit.mscontractservicee.enums.MilestoneStatus;
import tn.esprit.mscontractservicee.feign.UserServiceClient;
import tn.esprit.mscontractservicee.dto.UserDTO;
import tn.esprit.mscontractservicee.repository.EscrowAccountRepository;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;
import tn.esprit.mscontractservicee.service.email.AppEmailService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaSchedulerServiceTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private IMilestoneService milestoneService;

    @Mock
    private IPaymentService paymentService;

    @Mock
    private AppEmailService emailService;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private EscrowAccountRepository escrowAccountRepository;

    @InjectMocks
    private SlaSchedulerService slaSchedulerService;

    @Test
    void testCheckFreelancerDeadlines_DeadlineExceeded_CancelsMilestone() {
        // Contrat avec SLA 24h
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        contract.setSlaFreelancerHeures(24);

        // Jalon IN_PROGRESS avec deadline dépassée il y a 2 jours
        Milestone milestone = new Milestone();
        milestone.setId(1L);
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        milestone.setDeadline(LocalDate.now().minusDays(2)); // -48h
        milestone.setContract(contract);

        EscrowAccount escrow = new EscrowAccount();
        escrow.setStatus(EscrowStatus.LOCKED); // Pas de litige

        when(milestoneRepository.findByStatus(MilestoneStatus.IN_PROGRESS))
                .thenReturn(Arrays.asList(milestone));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.of(escrow));

        slaSchedulerService.checkFreelancerDeadlines();

        verify(paymentService, times(1)).refundMilestoneToClient(1L);
    }

    @Test
    void testCheckFreelancerDeadlines_EscrowDisputed_SkipsMilestone() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setSlaFreelancerHeures(24);

        Milestone milestone = new Milestone();
        milestone.setId(1L);
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        milestone.setDeadline(LocalDate.now().minusDays(2));
        milestone.setContract(contract);

        EscrowAccount escrow = new EscrowAccount();
        escrow.setStatus(EscrowStatus.DISPUTED); // Litige en cours

        when(milestoneRepository.findByStatus(MilestoneStatus.IN_PROGRESS))
                .thenReturn(Arrays.asList(milestone));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.of(escrow));

        slaSchedulerService.checkFreelancerDeadlines();

        // Ne doit PAS rembourser si litige en cours
        verify(paymentService, never()).refundMilestoneToClient(anyLong());
    }

    @Test
    void testCheckClientApprovals_SlaExceeded_AutoApproves() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setFreelancerCin(456L);
        contract.setSlaClientJours(7);

        // Jalon soumis il y a 8 jours
        Milestone milestone = new Milestone();
        milestone.setId(2L);
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setSubmittedAt(LocalDateTime.now().minusDays(8));
        milestone.setContract(contract);

        EscrowAccount escrow = new EscrowAccount();
        escrow.setStatus(EscrowStatus.LOCKED);

        when(milestoneRepository.findByStatus(MilestoneStatus.SUBMITTED))
                .thenReturn(Arrays.asList(milestone));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.of(escrow));

        slaSchedulerService.checkClientApprovals();

        verify(milestoneService, times(1)).autoApproveMilestone(2L, 0L);
    }

    @Test
    void testCheckClientApprovals_NoEscrow_Skips() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setSlaClientJours(7);

        Milestone milestone = new Milestone();
        milestone.setId(2L);
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setSubmittedAt(LocalDateTime.now().minusDays(8));
        milestone.setContract(contract);

        when(milestoneRepository.findByStatus(MilestoneStatus.SUBMITTED))
                .thenReturn(Arrays.asList(milestone));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.empty());

        slaSchedulerService.checkClientApprovals();

        // autoApprove doit quand même être appelé si l'escrow est absent
        verify(milestoneService, times(1)).autoApproveMilestone(2L, 0L);
    }

    @Test
    void testCheckClientApprovals_DisputedEscrow_Skips() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setSlaClientJours(7);

        Milestone milestone = new Milestone();
        milestone.setId(2L);
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setSubmittedAt(LocalDateTime.now().minusDays(8));
        milestone.setContract(contract);

        EscrowAccount escrow = new EscrowAccount();
        escrow.setStatus(EscrowStatus.DISPUTED);

        when(milestoneRepository.findByStatus(MilestoneStatus.SUBMITTED))
                .thenReturn(Arrays.asList(milestone));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.of(escrow));

        slaSchedulerService.checkClientApprovals();

        verify(milestoneService, never()).autoApproveMilestone(anyLong(), anyLong());
    }

    @Test
    void testCheckFreelancerDeadlines_RefundThrows_IsCaught() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setSlaFreelancerHeures(24);

        Milestone milestone = new Milestone();
        milestone.setId(1L);
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        milestone.setDeadline(LocalDate.now().minusDays(2));
        milestone.setContract(contract);

        EscrowAccount escrow = new EscrowAccount();
        escrow.setStatus(EscrowStatus.LOCKED);

        when(milestoneRepository.findByStatus(MilestoneStatus.IN_PROGRESS))
                .thenReturn(Arrays.asList(milestone));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.of(escrow));
        doThrow(new RuntimeException("boom")).when(paymentService).refundMilestoneToClient(1L);

        slaSchedulerService.checkFreelancerDeadlines();

        verify(paymentService).refundMilestoneToClient(1L);
    }

    @Test
    void testCheckClientApprovals_SubmittedAtNull_Skips() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setSlaClientJours(7);

        Milestone milestone = new Milestone();
        milestone.setId(2L);
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setSubmittedAt(null);
        milestone.setContract(contract);

        when(milestoneRepository.findByStatus(MilestoneStatus.SUBMITTED))
                .thenReturn(Arrays.asList(milestone));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.empty());

        slaSchedulerService.checkClientApprovals();

        verify(milestoneService, never()).autoApproveMilestone(anyLong(), anyLong());
        verifyNoInteractions(emailService);
    }

    @Test
    void testCheckClientApprovals_Within24Hours_SendsWarningEmail() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setSlaClientJours(7);

        Milestone milestone = new Milestone();
        milestone.setId(2L);
        milestone.setTitre("M1");
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setSubmittedAt(LocalDateTime.now().minusDays(6).minusHours(1));
        milestone.setContract(contract);

        UserDTO client = new UserDTO();
        client.setEmail("client@test.com");

        when(milestoneRepository.findByStatus(MilestoneStatus.SUBMITTED))
                .thenReturn(Arrays.asList(milestone));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.empty());
        when(userServiceClient.getPublicUserByCin(123L)).thenReturn(client);

        slaSchedulerService.checkClientApprovals();

        verify(emailService).sendSimpleEmail(eq("client@test.com"), anyString(), anyString());
        verify(milestoneService, never()).autoApproveMilestone(anyLong(), anyLong());
    }

    @Test
    void testCheckClientApprovals_Within24Hours_EmailSendThrows_IsCaught() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setSlaClientJours(7);

        Milestone milestone = new Milestone();
        milestone.setId(2L);
        milestone.setTitre("M1");
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setSubmittedAt(LocalDateTime.now().minusDays(6).minusHours(1));
        milestone.setContract(contract);

        UserDTO client = new UserDTO();
        client.setEmail("client@test.com");

        when(milestoneRepository.findByStatus(MilestoneStatus.SUBMITTED))
                .thenReturn(Arrays.asList(milestone));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.empty());
        when(userServiceClient.getPublicUserByCin(123L)).thenReturn(client);
        doThrow(new RuntimeException("smtp down")).when(emailService).sendSimpleEmail(anyString(), anyString(), anyString());

        slaSchedulerService.checkClientApprovals();

        verify(emailService).sendSimpleEmail(eq("client@test.com"), anyString(), anyString());
    }

    @Test
    void testCheckClientApprovals_UserNotFound_SkipsEmail() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setSlaClientJours(7);

        Milestone milestone = new Milestone();
        milestone.setId(2L);
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setSubmittedAt(LocalDateTime.now().minusDays(6).minusHours(1));
        milestone.setContract(contract);

        when(milestoneRepository.findByStatus(MilestoneStatus.SUBMITTED))
                .thenReturn(Arrays.asList(milestone));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.empty());
        when(userServiceClient.getPublicUserByCin(123L)).thenReturn(null);

        slaSchedulerService.checkClientApprovals();

        verifyNoInteractions(emailService);
    }

    @Test
    void testCheckClientApprovals_UserHasNoEmail_SkipsEmail() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(123L);
        contract.setSlaClientJours(7);

        Milestone milestone = new Milestone();
        milestone.setId(2L);
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setSubmittedAt(LocalDateTime.now().minusDays(6).minusHours(1));
        milestone.setContract(contract);

        UserDTO client = new UserDTO();
        client.setEmail(null);

        when(milestoneRepository.findByStatus(MilestoneStatus.SUBMITTED))
                .thenReturn(Arrays.asList(milestone));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.empty());
        when(userServiceClient.getPublicUserByCin(123L)).thenReturn(client);

        slaSchedulerService.checkClientApprovals();

        verifyNoInteractions(emailService);
    }

    @Test
    void testCheckClientApprovals_AutoApproveThrows_IsCaught() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setSlaClientJours(7);

        Milestone milestone = new Milestone();
        milestone.setId(2L);
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setSubmittedAt(LocalDateTime.now().minusDays(8));
        milestone.setContract(contract);

        when(milestoneRepository.findByStatus(MilestoneStatus.SUBMITTED))
                .thenReturn(Arrays.asList(milestone));
        when(escrowAccountRepository.findByContractId(10L)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("boom")).when(milestoneService).autoApproveMilestone(2L, 0L);

        slaSchedulerService.checkClientApprovals();

        verify(milestoneService).autoApproveMilestone(2L, 0L);
    }
}
