package tn.esprit.mscontractservicee.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.dto.ContractFinancialMetricsResponse;
import tn.esprit.mscontractservicee.dto.ContractWalletIdsResponse;
import tn.esprit.mscontractservicee.dto.ai.ContractAiPromptRequest;
import tn.esprit.mscontractservicee.dto.ai.ContractAiResponse;
import tn.esprit.mscontractservicee.dto.contract.ContractCreateRequest;
import tn.esprit.mscontractservicee.dto.contract.ContractResponse;
import tn.esprit.mscontractservicee.dto.contract.ContractUpdateRequest;
import tn.esprit.mscontractservicee.dto.signing.ContractSignatureStatusResponse;
import tn.esprit.mscontractservicee.dto.signing.SignatureRequestCreateResponse;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.entity.SignatureRequest;
import tn.esprit.mscontractservicee.entity.SignatureSigner;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.enums.SignerRole;
import tn.esprit.mscontractservicee.enums.SignatureRequestStatus;
import tn.esprit.mscontractservicee.enums.SignatureSignerStatus;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;
import tn.esprit.mscontractservicee.repository.SignatureRequestRepository;
import tn.esprit.mscontractservicee.repository.SignatureSignerRepository;
import tn.esprit.mscontractservicee.service.ContractTotalService;
import tn.esprit.mscontractservicee.service.IContractAiGenerationService;
import tn.esprit.mscontractservicee.service.IContractService;
import tn.esprit.mscontractservicee.service.ISignatureRequestService;
import tn.esprit.mscontractservicee.service.document.ContractDocumentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractControllerTest {

    @Mock
    private IContractService contractService;

    @Mock
    private ISignatureRequestService signatureRequestService;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private ContractDocumentService contractDocumentService;

    @Mock
    private SignatureRequestRepository signatureRequestRepository;

    @Mock
    private SignatureSignerRepository signatureSignerRepository;

    @Mock
    private IContractAiGenerationService contractAiService;

    @Mock
    private ContractTotalService contractTotalService;

    @InjectMocks
    private ContractController controller;

    private static Authentication auth(long cin, String... roles) {
        List<SimpleGrantedAuthority> authorities = roles == null
                ? List.of()
                : java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(String.valueOf(cin), "n/a", authorities);
    }

    private static Authentication authRaw(String principal, String... roles) {
        List<SimpleGrantedAuthority> authorities = roles == null
                ? List.of()
                : java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(principal, "n/a", authorities);
    }

    @Test
    void testHealthTestEndpoint() {
        ResponseEntity<Map<String, String>> res = controller.test();
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
        assertEquals("OK", res.getBody().get("status"));
    }

    @Test
    void testGenerateContractDraft_DelegatesToService() {
        ContractAiPromptRequest req = new ContractAiPromptRequest();
        req.setPrompt("prompt");

        ContractAiResponse ai = ContractAiResponse.builder()
                .projectTitle("p")
                .description("d")
                .montantTotal(new BigDecimal("100"))
                .build();

        when(contractAiService.generateContractDraft(any())).thenReturn(ai);

        ResponseEntity<ContractAiResponse> res = controller.generateContractDraft(req);
        assertEquals(200, res.getStatusCode().value());
        assertEquals("p", res.getBody().getProjectTitle());
    }

    @Test
    void testCreateContract_ReturnsCreated() {
        ContractCreateRequest req = new ContractCreateRequest();
        req.setFreelancerCin(200L);
        req.setProjectTitle("t");
        req.setMontantTotal(new BigDecimal("1000"));

        Contract saved = new Contract();
        saved.setId(1L);
        saved.setReference("CTR-1");
        saved.setClientCin(100L);
        saved.setFreelancerCin(200L);
        saved.setStatus(ContractStatus.DRAFT);

        when(contractService.createContract(any(Contract.class), eq(100L))).thenReturn(saved);

        ResponseEntity<ContractResponse> res = controller.createContract(auth(100, "ROLE_CLIENT"), req);
        assertEquals(201, res.getStatusCode().value());
        assertNotNull(res.getBody());
        assertEquals(1L, res.getBody().getId());
        assertEquals("CTR-1", res.getBody().getReference());
    }

    @Test
    void testGetContractById_FreelancerCannotAccessUnsignedContract() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        contract.setDateSignature(null);

        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        Authentication authentication = auth(200, "ROLE_FREELANCER");
        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.getContractById(authentication, 1L);
            }
        });
    }

    @Test
    void testGetContractById_ClientCanAccess() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);

        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        ResponseEntity<Contract> res = controller.getContractById(auth(100, "ROLE_CLIENT"), 1L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(1L, res.getBody().getId());
    }

    @Test
    void testGetContractWalletIds_ReturnsServiceResponse() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        contract.setDateSignature(LocalDateTime.now());

        when(contractService.findById(1L)).thenReturn(Optional.of(contract));
        when(contractService.getWalletIds(1L)).thenReturn(ContractWalletIdsResponse.builder()
                .contractId(1L)
                .clientCin(100L)
                .clientWalletCin(10L)
                .freelancerCin(200L)
                .freelancerWalletCin(20L)
                .build());

        ResponseEntity<ContractWalletIdsResponse> res = controller.getContractWalletIds(auth(100, "ROLE_CLIENT"), 1L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(10L, res.getBody().getClientWalletCin());
    }

    @Test
    void testGetContractFinancialMetrics_Delegates() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        contract.setDateSignature(LocalDateTime.now());

        ContractFinancialMetricsResponse metrics = ContractFinancialMetricsResponse.builder()
                .contractId(1L)
                .readyToFinalize(true)
                .build();

        when(contractService.findById(1L)).thenReturn(Optional.of(contract));
        when(contractTotalService.getFinancialMetrics(1L)).thenReturn(metrics);

        ResponseEntity<ContractFinancialMetricsResponse> res = controller.getContractFinancialMetrics(auth(100, "ROLE_CLIENT"), 1L);
        assertEquals(200, res.getStatusCode().value());
        assertTrue(res.getBody().isReadyToFinalize());
    }

    @Test
    void testGetMyContracts_AdminUsesFindAll() {
        Page<Contract> page = Page.empty(PageRequest.of(0, 10));
        when(contractService.findAll(any())).thenReturn(page);

        ResponseEntity<Page<Contract>> res = controller.getMyContracts(auth(1, "ROLE_ADMIN"), 0, 10);
        assertEquals(200, res.getStatusCode().value());
        verify(contractService).findAll(any());
    }

    @Test
    void testGetMyContracts_ClientUsesFindByClientCin() {
        Page<Contract> page = Page.empty(PageRequest.of(0, 10));
        when(contractService.findByClientCin(eq(100L), any())).thenReturn(page);

        ResponseEntity<Page<Contract>> res = controller.getMyContracts(auth(100, "ROLE_CLIENT"), 0, 10);
        assertEquals(200, res.getStatusCode().value());
        verify(contractService).findByClientCin(eq(100L), any());
    }

    @Test
    void testGetMyContracts_FreelancerUsesFindSignedByFreelancerCin() {
        Page<Contract> page = Page.empty(PageRequest.of(0, 10));
        when(contractService.findSignedByFreelancerCin(eq(200L), any())).thenReturn(page);

        ResponseEntity<Page<Contract>> res = controller.getMyContracts(auth(200, "ROLE_FREELANCER"), 0, 10);
        assertEquals(200, res.getStatusCode().value());
        verify(contractService).findSignedByFreelancerCin(eq(200L), any());
    }

    @Test
    void testGetMyContracts_UnsupportedRole_ThrowsForbidden() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.getMyContracts(auth(123, "ROLE_OTHER"), 0, 10);
            }
        });
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void testGetMyContracts_InvalidAuthenticatedCin_ThrowsUnauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.getMyContracts(authRaw("abc", "ROLE_CLIENT"), 0, 10);
            }
        });
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void testGetSignedContractsByFreelancer_ForbiddenWhenNotSelf() {
        Authentication authentication = auth(200, "ROLE_FREELANCER");
        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.getSignedContractsByFreelancer(authentication, 999L, 0, 10);
            }
        });
    }

    @Test
    void testGetAllContracts_AdminCanFilterByUserCin() {
        Page<Contract> page = new PageImpl<>(List.of(new Contract()));
        when(contractService.findByUserCin(eq(123L), any())).thenReturn(page);

        ResponseEntity<Page<Contract>> res = controller.getAllContracts(auth(1, "ROLE_ADMIN"), 123L, null, 0, 10);
        assertEquals(200, res.getStatusCode().value());
        verify(contractService).findByUserCin(eq(123L), any());
    }

    @Test
    void testUpdateContract_ForbidsNonOwnerNonAdmin() {
        Contract existing = new Contract();
        existing.setId(1L);
        existing.setClientCin(100L);

        when(contractService.findById(1L)).thenReturn(Optional.of(existing));

        Authentication authentication = auth(999, "ROLE_CLIENT");
        ContractUpdateRequest req = new ContractUpdateRequest();
        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.updateContract(authentication, 1L, req);
            }
        });
    }

    @Test
    void testUpdateContract_AllowsOwner() {
        Contract existing = new Contract();
        existing.setId(1L);
        existing.setClientCin(100L);

        when(contractService.findById(1L)).thenReturn(Optional.of(existing));
        when(contractService.updateContract(eq(1L), any(Contract.class))).thenAnswer(inv -> {
            Contract c = inv.getArgument(1);
            c.setId(1L);
            c.setClientCin(100L);
            c.setReference("CTR-1");
            return c;
        });

        ResponseEntity<ContractResponse> res = controller.updateContract(auth(100, "ROLE_CLIENT"), 1L, new ContractUpdateRequest());
        assertEquals(200, res.getStatusCode().value());
        assertEquals(1L, res.getBody().getId());
    }

    @Test
    void testUpdateContractStatus_AllowsOwner() {
        Contract existing = new Contract();
        existing.setId(1L);
        existing.setClientCin(100L);
        when(contractService.findById(1L)).thenReturn(Optional.of(existing));

        Contract updated = new Contract();
        updated.setId(1L);
        updated.setStatus(ContractStatus.CANCELLED);
        when(contractService.updateStatus(1L, ContractStatus.CANCELLED)).thenReturn(updated);

        ResponseEntity<Contract> res = controller.updateContractStatus(auth(100, "ROLE_CLIENT"), 1L, ContractStatus.CANCELLED);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(ContractStatus.CANCELLED, res.getBody().getStatus());
    }

    @Test
    void testDeleteContract_NoContent() {
        Contract existing = new Contract();
        existing.setId(1L);
        existing.setClientCin(100L);
        when(contractService.findById(1L)).thenReturn(Optional.of(existing));

        doNothing().when(contractService).deleteContract(1L);

        ResponseEntity<Void> res = controller.deleteContract(auth(100, "ROLE_CLIENT"), 1L);
        assertEquals(204, res.getStatusCode().value());
        verify(contractService).deleteContract(1L);
    }

    @Test
    void testFinalizeContract_OwnerAllowed() {
        Contract existing = new Contract();
        existing.setId(1L);
        existing.setClientCin(100L);
        when(contractService.findById(1L)).thenReturn(Optional.of(existing));

        Contract finalized = new Contract();
        finalized.setId(1L);
        finalized.setStatus(ContractStatus.PENDING_SIGNATURE);
        when(contractService.finalizeForSignature(1L)).thenReturn(finalized);

        ResponseEntity<Contract> res = controller.finalizeContract(auth(100, "ROLE_CLIENT"), 1L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(ContractStatus.PENDING_SIGNATURE, res.getBody().getStatus());
    }

    @Test
    void testCreateSignatureRequest_Delegates() {
        Contract existing = new Contract();
        existing.setId(1L);
        existing.setClientCin(100L);
        when(contractService.findById(1L)).thenReturn(Optional.of(existing));

        SignatureRequestCreateResponse out = SignatureRequestCreateResponse.builder()
                .signatureRequestId(10L)
                .build();
        when(signatureRequestService.createAndSendForContract(1L)).thenReturn(out);

        ResponseEntity<SignatureRequestCreateResponse> res = controller.createSignatureRequest(auth(100, "ROLE_CLIENT"), 1L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(10L, res.getBody().getSignatureRequestId());
    }

    @Test
    void testDownloadContractDocument_ReturnsPdfWithHeaders() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setReference("CTR-1");
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        when(milestoneRepository.findByContractIdOrderByOrdreAsc(1L)).thenReturn(List.of(new Milestone()));

        SignatureRequest req = new SignatureRequest();
        req.setId(10L);
        req.setContractId(1L);
        when(signatureRequestRepository.findTopByContractIdOrderByIdDesc(1L)).thenReturn(Optional.of(req));
        when(signatureSignerRepository.findBySignatureRequestId(10L)).thenReturn(Collections.emptyList());

        byte[] pdf = new byte[]{1, 2, 3};
        when(contractDocumentService.generateContractPdf(eq(contract), anyList(), anyList())).thenReturn(pdf);

        ResponseEntity<byte[]> res = controller.downloadContractDocument(auth(100, "ROLE_CLIENT"), 1L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PDF, res.getHeaders().getContentType());
        assertTrue(res.getHeaders().containsKey(HttpHeaders.CONTENT_DISPOSITION));
        assertArrayEquals(pdf, res.getBody());
    }

    @Test
    void testSignatureStatus_ReturnsSignerViews() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        SignatureRequest req = new SignatureRequest();
        req.setId(10L);
        req.setContractId(1L);
        req.setStatus(SignatureRequestStatus.COMPLETED);
        when(signatureRequestRepository.findTopByContractIdOrderByIdDesc(1L)).thenReturn(Optional.of(req));

        SignatureSigner signer = new SignatureSigner();
        signer.setRole(SignerRole.CLIENT);
        signer.setSignerEmail("a@b.c");
        signer.setStatus(SignatureSignerStatus.SIGNED);
        when(signatureSignerRepository.findBySignatureRequestId(10L)).thenReturn(List.of(signer));

        ResponseEntity<ContractSignatureStatusResponse> res = controller.signatureStatus(auth(100, "ROLE_CLIENT"), 1L);
        assertEquals(200, res.getStatusCode().value());
        assertTrue(res.getBody().isFullySigned());
        assertEquals(1, res.getBody().getSigners().size());
    }
    @Test
    void testGetSignedContractsByFreelancer_ForbiddenWhenMismatch() {
        Authentication authentication = auth(999, "ROLE_FREELANCER");
        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.getSignedContractsByFreelancer(authentication, 111L, 0, 10);
            }
        });
    }

    @Test
    void testGetSignedContractsByFreelancer_AdminAllowed() {
        Page<Contract> page = new PageImpl<>(List.of(new Contract()));
        when(contractService.findSignedByFreelancerCin(eq(111L), any())).thenReturn(page);

        ResponseEntity<Page<Contract>> res = controller.getSignedContractsByFreelancer(auth(1, "ROLE_ADMIN"), 111L, 0, 10);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().getTotalElements());
    }

    @Test
    void testGetAllContracts_AdminWithUserCin_Delegates() {
        Page<Contract> page = new PageImpl<>(List.of(new Contract()));
        when(contractService.findByUserCin(eq(500L), any())).thenReturn(page);

        ResponseEntity<Page<Contract>> res = controller.getAllContracts(auth(1, "ROLE_ADMIN"), 500L, null, 0, 10);
        assertEquals(200, res.getStatusCode().value());
        verify(contractService).findByUserCin(eq(500L), any());
    }

    @Test
    void testGetAllContracts_AdminWithFreelancerCin_Delegates() {
        Page<Contract> page = new PageImpl<>(List.of(new Contract()));
        when(contractService.findSignedByFreelancerCin(eq(600L), any())).thenReturn(page);

        ResponseEntity<Page<Contract>> res = controller.getAllContracts(auth(1, "ROLE_ADMIN"), null, 600L, 0, 10);
        assertEquals(200, res.getStatusCode().value());
        verify(contractService).findSignedByFreelancerCin(eq(600L), any());
    }
    @Test
    void testDeleteContract_AdminAllowed() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));
        doNothing().when(contractService).deleteContract(1L);

        ResponseEntity<Void> res = controller.deleteContract(auth(1, "ROLE_ADMIN"), 1L);
        assertEquals(204, res.getStatusCode().value());
        verify(contractService).deleteContract(1L);
    }
}
