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
import tn.esprit.mscontractservicee.dto.dispute.DisputeCreateRequest;
import tn.esprit.mscontractservicee.entity.*;
import tn.esprit.mscontractservicee.enums.*;
import tn.esprit.mscontractservicee.repository.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisputeServiceImplExtraTest {

    @Mock
    private DisputeRepository disputeRepository;
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
    private INotificationService notificationService;

    @InjectMocks
    private DisputeServiceImpl disputeService;

    @Test
    void testOpenDispute_ContractNotActive_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        contract.setStatus(ContractStatus.DRAFT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setContractId(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> disputeService.openDispute(100L, req));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testOpenDispute_EscrowNotFound_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(escrowAccountRepository.findByContractId(1L)).thenReturn(Optional.empty());

        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setContractId(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> disputeService.openDispute(100L, req));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testRequireParticipant_NotParticipant_Throws() {
        Contract contract = new Contract();
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> ReflectionTestUtils.invokeMethod(disputeService, "requireParticipant", contract, 999L));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void testValidateOpenDisputeRequest_NullContractId_Throws() {
        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setContractId(null);
        
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> ReflectionTestUtils.invokeMethod(disputeService, "validateOpenDisputeRequest", 100L, req));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testValidateDisputeUniquenessAndMilestone_GlobalDisputeDuplicate_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        when(disputeRepository.existsByContractIdAndStatusIn(anyLong(), anySet())).thenReturn(true);
        
        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setContractId(1L);
        req.setMilestoneId(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> ReflectionTestUtils.invokeMethod(disputeService, "validateDisputeUniquenessAndMilestone", contract, req));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void testValidateDisputeUniquenessAndMilestone_MilestoneMismatch_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(2L); // Different
        when(milestoneRepository.findById(10L)).thenReturn(Optional.of(milestone));

        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setMilestoneId(10L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> ReflectionTestUtils.invokeMethod(disputeService, "validateDisputeUniquenessAndMilestone", contract, req));
        assertEquals(400, ex.getStatusCode().value());
    }
}
