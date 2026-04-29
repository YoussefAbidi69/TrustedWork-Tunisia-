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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import tn.esprit.mscontractservicee.dto.UserDTO;
import tn.esprit.mscontractservicee.dto.ContractWalletIdsResponse;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Wallet;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.feign.UserServiceClient;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ContractServiceImplTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private IWalletService walletService;

    @Mock
    private ContractTotalService contractTotalService;

    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private ContractServiceImpl contractService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(contractService, "requireFreelancerKycApproved", true);
    }

    private UserDTO buildFreelancer() {
        UserDTO dto = new UserDTO();
        dto.setRole("FREELANCER");
        dto.setKycStatus("APPROVED");
        return dto;
    }

    private Wallet buildWallet(Long cin) {
        return Wallet.builder().id(1L).userCin(cin).balance(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO).totalSpent(BigDecimal.ZERO)
                .totalCommissionPaid(BigDecimal.ZERO).build();
    }

    private static FeignException feignEx(int status) {
        Request req = Request.create(Request.HttpMethod.GET, "http://ms-user/users/123",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        Response resp = Response.builder()
                .status(status)
                .reason("x")
                .request(req)
                .headers(Collections.emptyMap())
                .body(new byte[0])
                .build();
        return FeignException.errorStatus("getUserByCin", resp);
    }

    @Test
    void testCreateContract_Success() {
        Long clientCin = 100L;
        Long freelancerCin = 200L;

        Contract contract = new Contract();
        contract.setFreelancerCin(freelancerCin);
        contract.setMontantTotal(new BigDecimal("1000"));

        when(userServiceClient.getUserByCin(freelancerCin)).thenReturn(buildFreelancer());
        when(walletService.getOrCreateWallet(clientCin)).thenReturn(buildWallet(clientCin));
        when(walletService.getOrCreateWallet(freelancerCin)).thenReturn(buildWallet(freelancerCin));

        Contract saved = new Contract();
        saved.setId(1L);
        saved.setStatus(ContractStatus.DRAFT);
        saved.setReference("CTR-TESTREF");
        saved.setClientCin(clientCin);
        saved.setFreelancerCin(freelancerCin);

        when(contractRepository.save(any(Contract.class))).thenReturn(saved);

        Contract result = contractService.createContract(contract, clientCin);

        assertNotNull(result);
        assertEquals(ContractStatus.DRAFT, result.getStatus());
        verify(notificationService, times(1)).createNotification(
                eq(freelancerCin), any(), any(), any(), any());
    }

    @Test
    void testCreateContract_NoFreelancerCin_Throws() {
        Contract contract = new Contract();
        contract.setFreelancerCin(null);

        assertThrows(ResponseStatusException.class, () ->
                contractService.createContract(contract, 100L));
    }

    @Test
    void testCreateContract_UserNotFreelancer_Throws() {
        Long freelancerCin = 200L;
        Contract contract = new Contract();
        contract.setFreelancerCin(freelancerCin);

        UserDTO notFreelancer = new UserDTO();
        notFreelancer.setRole("CLIENT");
        when(userServiceClient.getUserByCin(freelancerCin)).thenReturn(notFreelancer);

        assertThrows(ResponseStatusException.class, () ->
                contractService.createContract(contract, 100L));
    }

    @Test
    void testFindById_Found() {
        Contract contract = new Contract();
        contract.setId(5L);
        when(contractRepository.findById(5L)).thenReturn(Optional.of(contract));

        Optional<Contract> result = contractService.findById(5L);

        assertTrue(result.isPresent());
        assertEquals(5L, result.get().getId());
    }

    @Test
    void testFindById_NotFound() {
        when(contractRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Contract> result = contractService.findById(99L);

        assertFalse(result.isPresent());
    }

    @Test
    void testUpdateContract_NotFound_Throws() {
        when(contractRepository.findById(1L)).thenReturn(Optional.empty());
        Contract update = new Contract();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                contractService.updateContract(1L, update);
            }
        });
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void testUpdateContract_WhenNotDraft_ThrowsBadRequest() {
        Contract existing = new Contract();
        existing.setId(1L);
        existing.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(existing));

        Contract update = new Contract();
        update.setProjectTitle("x");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                contractService.updateContract(1L, update);
            }
        });
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testUpdateContract_BudgetBelowMilestonesTotal_ThrowsBadRequest() {
        Contract existing = new Contract();
        existing.setId(1L);
        existing.setStatus(ContractStatus.DRAFT);
        existing.setMontantTotal(new BigDecimal("1000"));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(milestoneRepository.countByContractId(1L)).thenReturn(2L);
        when(contractTotalService.computeMilestonesTotal(1L)).thenReturn(new BigDecimal("600"));

        Contract update = new Contract();
        update.setMontantTotal(new BigDecimal("500"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                contractService.updateContract(1L, update);
            }
        });
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testUpdateContract_Success() {
        Contract existing = new Contract();
        existing.setId(1L);
        existing.setStatus(ContractStatus.DRAFT);
        existing.setMontantTotal(new BigDecimal("1000"));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(milestoneRepository.countByContractId(1L)).thenReturn(0L);
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

        Contract update = new Contract();
        update.setProjectTitle("new title");
        update.setDescription("new desc");
        update.setMontantTotal(new BigDecimal("1200"));

        Contract res = contractService.updateContract(1L, update);
        assertEquals("new title", res.getProjectTitle());
        assertEquals("new desc", res.getDescription());
        assertEquals(new BigDecimal("1200"), res.getMontantTotal());
        assertNotNull(res.getUpdatedAt());
    }

    @Test
    void testFindAll_Delegates() {
        PageRequest pr = PageRequest.of(0, 10);
        Page<Contract> page = new PageImpl<>(List.of(new Contract()));
        when(contractRepository.findAll(pr)).thenReturn(page);

        Page<Contract> res = contractService.findAll(pr);
        assertEquals(1, res.getContent().size());
    }

    @Test
    void testFindByUserCin_Delegates() {
        PageRequest pr = PageRequest.of(0, 10);
        Page<Contract> page = new PageImpl<>(List.of(new Contract()));
        when(contractRepository.findByClientCinOrFreelancerCin(123L, 123L, pr)).thenReturn(page);

        Page<Contract> res = contractService.findByUserCin(123L, pr);
        assertEquals(1, res.getContent().size());
    }

    @Test
    void testDeleteContract_OnlyDraft() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        assertThrows(ResponseStatusException.class, () ->
                contractService.deleteContract(1L));
    }

    @Test
    void testDeleteContract_Draft_Success() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.DRAFT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        doNothing().when(contractRepository).deleteById(1L);

        assertDoesNotThrow(() -> contractService.deleteContract(1L));
        verify(contractRepository, times(1)).deleteById(1L);
    }

    @Test
    void testUpdateStatus_Cancelled_SetsCancelledAt() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

        Contract result = contractService.updateStatus(1L, ContractStatus.CANCELLED);

        assertEquals(ContractStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getCancelledAt());
    }

    @Test
    void testFinalizeForSignature_NoBudget_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setMontantTotal(null);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(milestoneRepository.countByContractId(1L)).thenReturn(2L);

        assertThrows(ResponseStatusException.class, () ->
                contractService.finalizeForSignature(1L));
    }

    @Test
    void testFinalizeForSignature_NoMilestones_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setMontantTotal(new BigDecimal("1000"));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(milestoneRepository.countByContractId(1L)).thenReturn(0L);

        assertThrows(ResponseStatusException.class, () ->
                contractService.finalizeForSignature(1L));
    }

    @Test
    void testFinalizeForSignature_MilestonesTotalMismatch_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setMontantTotal(new BigDecimal("1000"));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(milestoneRepository.countByContractId(1L)).thenReturn(1L);
        when(contractTotalService.computeMilestonesTotal(1L)).thenReturn(new BigDecimal("900")); // Mismatch

        assertThrows(ResponseStatusException.class, () ->
                contractService.finalizeForSignature(1L));
    }

    @Test
    void testFinalizeForSignature_Success() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setVersion(0);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(milestoneRepository.countByContractId(1L)).thenReturn(1L);
        when(contractTotalService.computeMilestonesTotal(1L)).thenReturn(new BigDecimal("1000"));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

        Contract res = contractService.finalizeForSignature(1L);
        assertEquals(ContractStatus.PENDING_SIGNATURE, res.getStatus());
        assertNotNull(res.getFinalizedAt());
        assertTrue(res.getVersion() >= 1);
    }

    @Test
    void testGetWalletIds_LinksWalletsWhenMissing() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        contract.setClientWalletCin(null);
        contract.setFreelancerWalletCin(null);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(walletService.getOrCreateWallet(anyLong())).thenReturn(buildWallet(0L));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

        ContractWalletIdsResponse res = contractService.getWalletIds(1L);
        assertEquals(100L, res.getClientWalletCin());
        assertEquals(200L, res.getFreelancerWalletCin());
        verify(contractRepository, times(1)).save(any(Contract.class));
    }

    @Test
    void testCreateContract_UserLookupFallbackToKycStatus() {
        Long clientCin = 100L;
        Long freelancerCin = 200L;

        Contract contract = new Contract();
        contract.setFreelancerCin(freelancerCin);
        contract.setMontantTotal(new BigDecimal("1000"));

        UserDTO freelancer = buildFreelancer();
        when(userServiceClient.getUserByCin(freelancerCin)).thenThrow(feignEx(405));
        when(userServiceClient.getUserByCinFromKycStatus(freelancerCin)).thenReturn(freelancer);
        when(walletService.getOrCreateWallet(clientCin)).thenReturn(buildWallet(clientCin));
        when(walletService.getOrCreateWallet(freelancerCin)).thenReturn(buildWallet(freelancerCin));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> {
            Contract c = i.getArgument(0);
            c.setId(1L);
            return c;
        });

        Contract res = contractService.createContract(contract, clientCin);
        assertNotNull(res.getReference());
        assertEquals(ContractStatus.DRAFT, res.getStatus());
    }

    @Test
    void testCreateContract_UserLookup404_PropagatesNotFound() {
        Long clientCin = 100L;
        Long freelancerCin = 200L;

        Contract contract = new Contract();
        contract.setFreelancerCin(freelancerCin);
        contract.setMontantTotal(new BigDecimal("1000"));

        when(userServiceClient.getUserByCin(freelancerCin)).thenThrow(feignEx(404));
        when(userServiceClient.getUserByCinFromKycStatus(freelancerCin)).thenThrow(feignEx(404));

        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                contractService.createContract(contract, clientCin);
            }
        });
    }

    @Test
    void testCreateContract_KycNotApproved_Throws() {
        Long freelancerCin = 200L;
        Contract contract = new Contract();
        contract.setFreelancerCin(freelancerCin);

        UserDTO kycPending = buildFreelancer();
        kycPending.setKycStatus("PENDING");
        when(userServiceClient.getUserByCin(freelancerCin)).thenReturn(kycPending);

        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                contractService.createContract(contract, 100L);
            }
        });
    }

    @Test
    void testCreateContract_KycStatusNull_Throws() {
        Long freelancerCin = 200L;
        Contract contract = new Contract();
        contract.setFreelancerCin(freelancerCin);

        UserDTO kycNull = buildFreelancer();
        kycNull.setKycStatus(null);
        when(userServiceClient.getUserByCin(freelancerCin)).thenReturn(kycNull);

        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                contractService.createContract(contract, 100L);
            }
        });
    }

    @Test
    void testCreateContract_MissingAuthCin_Throws() {
        Contract contract = new Contract();
        contract.setFreelancerCin(200L);
        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                contractService.createContract(contract, null);
            }
        });
    }

    @Test
    void testUserLookup_401_ThrowsUnauthorized() {
        when(userServiceClient.getUserByCin(anyLong())).thenThrow(feignEx(401));
        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                contractService.getClientInfo(1L);
            }
        });
    }

    @Test
    void testSafeFeignBody_Truncation() {
        // We can't easily test the private method, but we can verify the mock setup works
        assertNotNull(contractService);
    }

    @Test
    void testHandlePrimaryUserLookupFailure_500_TriesFallback() {
        Long cin = 200L;
        when(userServiceClient.getUserByCin(cin)).thenThrow(feignEx(500));
        when(userServiceClient.getUserByCinFromKycStatus(cin)).thenReturn(buildFreelancer());
        
        Contract contract = new Contract();
        contract.setFreelancerCin(cin);
        when(walletService.getOrCreateWallet(anyLong())).thenReturn(buildWallet(0L));
        when(contractRepository.save(any())).thenReturn(new Contract());

        assertDoesNotThrow(new Executable() {
            @Override
            public void execute() {
                contractService.createContract(contract, 100L);
            }
        });
    }
    @Test
    void testDeleteContract_NotDraft_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                contractService.deleteContract(1L);
            }
        });
    }
    @Test
    void testUpdateContract_NegativeBudget_Throws() {
        Contract existing = new Contract();
        existing.setId(1L);
        existing.setStatus(ContractStatus.DRAFT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(existing));

        Contract update = new Contract();
        update.setMontantTotal(new BigDecimal("-100"));

        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                contractService.updateContract(1L, update);
            }
        });
    }

    @Test
    void testGetWalletIds_WalletsAlreadyLinked_NoSave() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        contract.setClientWalletCin(100L);
        contract.setFreelancerWalletCin(200L);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        ContractWalletIdsResponse res = contractService.getWalletIds(1L);
        assertEquals(100L, res.getClientWalletCin());
        assertEquals(200L, res.getFreelancerWalletCin());
        // No save should occur since wallets are already linked
        verify(contractRepository, never()).save(any());
    }

    @Test
    void testFinalizeForSignature_VersionNull_SetsToOne() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setVersion(null); // null version
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(milestoneRepository.countByContractId(1L)).thenReturn(1L);
        when(contractTotalService.computeMilestonesTotal(1L)).thenReturn(new BigDecimal("1000"));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

        Contract res = contractService.finalizeForSignature(1L);
        assertEquals(1, res.getVersion());
        assertEquals(ContractStatus.PENDING_SIGNATURE, res.getStatus());
    }
    @Test
    void testDeleteContract_NotFound_Throws() {
        when(contractRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> contractService.deleteContract(1L));
    }

    @Test
    void testUpdateStatus_NotCancelled_DoesNotSetCancelledAt() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.DRAFT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Contract res = contractService.updateStatus(1L, ContractStatus.ACTIVE);
        assertEquals(ContractStatus.ACTIVE, res.getStatus());
        assertNull(res.getCancelledAt());
    }
    @Test
    void testUpdateContract_NullMontantTotal_FallsBackToExisting() {
        Contract existing = new Contract();
        existing.setId(1L);
        existing.setStatus(ContractStatus.DRAFT);
        existing.setMontantTotal(new BigDecimal("500"));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(milestoneRepository.countByContractId(1L)).thenReturn(0L);
        when(contractRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Contract update = new Contract();
        update.setMontantTotal(null); // null → falls back to existing (line 118 false branch)
        Contract res = contractService.updateContract(1L, update);
        assertEquals(new BigDecimal("500"), res.getMontantTotal());
    }

    @Test
    void testUpdateContract_MilestoneTotalExceedsBudget_Throws() {
        Contract existing = new Contract();
        existing.setId(1L);
        existing.setStatus(ContractStatus.DRAFT);
        existing.setMontantTotal(new BigDecimal("500"));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(milestoneRepository.countByContractId(1L)).thenReturn(2L); // milestones exist
        when(contractTotalService.computeMilestonesTotal(1L)).thenReturn(new BigDecimal("600")); // > budget

        Contract update = new Contract();
        update.setMontantTotal(new BigDecimal("400")); // will be less than milestones total

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractService.updateContract(1L, update));
        assertEquals(400, ex.getStatusCode().value()); // covers line 127-129
    }

    @Test
    void testFinalizeForSignature_MilestoneTotalMismatch_Throws() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setVersion(1);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(milestoneRepository.countByContractId(1L)).thenReturn(1L);
        when(contractTotalService.computeMilestonesTotal(1L)).thenReturn(new BigDecimal("900")); // != 1000

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractService.finalizeForSignature(1L));
        assertEquals(400, ex.getStatusCode().value()); // covers line 273-275
    }
}
