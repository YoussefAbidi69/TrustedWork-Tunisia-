package tn.esprit.mscontractservicee.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.dto.UserDTO;
import tn.esprit.mscontractservicee.dto.signing.SignatureRequestCreateResponse;
import tn.esprit.mscontractservicee.dto.signing.SigningRequestViewResponse;
import tn.esprit.mscontractservicee.dto.signing.SigningSignRequest;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.SignatureRequest;
import tn.esprit.mscontractservicee.entity.SignatureSigner;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.enums.SignerRole;
import tn.esprit.mscontractservicee.enums.SignatureRequestStatus;
import tn.esprit.mscontractservicee.enums.SignatureSignerStatus;
import tn.esprit.mscontractservicee.enums.SignatureType;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;
import tn.esprit.mscontractservicee.repository.SignatureRequestRepository;
import tn.esprit.mscontractservicee.repository.SignatureSignerRepository;
import tn.esprit.mscontractservicee.service.email.AppEmailService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignatureRequestServiceImplTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private SignatureRequestRepository signatureRequestRepository;

    @Mock
    private SignatureSignerRepository signatureSignerRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private AppEmailService emailService;

    @Mock
    private IContractService contractService;

    @InjectMocks
    private SignatureRequestServiceImpl signatureService;

    private static String sha256Hex(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testCreateAndSendForContract_NotLocked() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setStatus(ContractStatus.DRAFT);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(contractService.finalizeForSignature(1L)).thenReturn(contract);

        assertThrows(ResponseStatusException.class, () -> signatureService.createAndSendForContract(1L));
    }

    @Test
    void testViewForToken_ReturnsView() {
        String token = "tok_123";

        SignatureRequest req = new SignatureRequest();
        req.setId(1L);
        req.setContractId(10L);
        req.setStatus(SignatureRequestStatus.SENT);
        req.setSnapshotHash("hash");
        req.setSnapshotJson("{}");
        when(signatureRequestRepository.findById(1L)).thenReturn(Optional.of(req));

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setReference("CTR-REF");
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        SignatureSigner signer = new SignatureSigner();
        signer.setRole(SignerRole.CLIENT);
        signer.setSignerEmail("client@test.com");
        signer.setStatus(SignatureSignerStatus.PENDING);
        signer.setTokenHash(sha256Hex(token));

        when(signatureSignerRepository.findBySignatureRequestId(1L)).thenReturn(java.util.List.of(signer));

        SigningRequestViewResponse view = signatureService.viewForToken(1L, token);
        assertNotNull(view);
        assertEquals(1L, view.getSignatureRequestId());
        assertEquals("CTR-REF", view.getContractReference());
        assertEquals("CLIENT", view.getSignerRole());
    }

    @Test
    void testCreateAndSendForContract_SendsEmailsAndMarksSent() {
        org.springframework.test.util.ReflectionTestUtils.setField(signatureService, "frontendBaseUrl", "http://localhost:4200");
        org.springframework.test.util.ReflectionTestUtils.setField(signatureService, "tokenTtlMinutes", 10L);

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        contract.setReference("CTR-REF");
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        contract.setVersion(1);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        when(signatureRequestRepository.findTopByContractIdOrderByIdDesc(10L)).thenReturn(Optional.empty());

        Milestone m = new Milestone();
        m.setId(1L);
        m.setContractId(10L);
        m.setTitre("M1");
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(10L)).thenReturn(List.of(m));

        tn.esprit.mscontractservicee.dto.UserDTO client = new tn.esprit.mscontractservicee.dto.UserDTO();
        client.setEmail("client@test.com");
        tn.esprit.mscontractservicee.dto.UserDTO freelancer = new tn.esprit.mscontractservicee.dto.UserDTO();
        freelancer.setEmail("freelancer@test.com");
        when(contractService.getClientInfo(10L)).thenReturn(client);
        when(contractService.getFreelancerInfo(10L)).thenReturn(freelancer);

        when(signatureRequestRepository.save(any(SignatureRequest.class))).thenAnswer(inv -> {
            SignatureRequest req = inv.getArgument(0);
            if (req.getId() == null) {
                req.setId(1L);
            }
            return req;
        });
        when(signatureSignerRepository.save(any(SignatureSigner.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendSignatureRequestEmail(anyString(), anyString(), anyString());

        var res = signatureService.createAndSendForContract(10L);
        assertNotNull(res);
        assertEquals("SENT", res.getStatus());
        assertTrue(res.isEmailsSent());
        verify(emailService, times(2)).sendSignatureRequestEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testCreateAndSendForContract_ExistingPreviousRequest_IsCancelled() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        contract.setReference("CTR-REF");
        contract.setVersion(1);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        SignatureRequest prev = new SignatureRequest();
        prev.setId(100L);
        prev.setStatus(SignatureRequestStatus.SENT);
        when(signatureRequestRepository.findTopByContractIdOrderByIdDesc(10L)).thenReturn(Optional.of(prev));

        Milestone m = new Milestone();
        m.setId(1L);
        m.setContractId(10L);
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(10L)).thenReturn(List.of(m));

        tn.esprit.mscontractservicee.dto.UserDTO client = new tn.esprit.mscontractservicee.dto.UserDTO();
        client.setEmail("c@t.com");
        tn.esprit.mscontractservicee.dto.UserDTO freelancer = new tn.esprit.mscontractservicee.dto.UserDTO();
        freelancer.setEmail("f@t.com");
        when(contractService.getClientInfo(10L)).thenReturn(client);
        when(contractService.getFreelancerInfo(10L)).thenReturn(freelancer);
        when(signatureRequestRepository.save(any(SignatureRequest.class))).thenAnswer(i -> i.getArgument(0));

        signatureService.createAndSendForContract(10L);

        assertEquals(SignatureRequestStatus.CANCELLED, prev.getStatus());
        verify(signatureRequestRepository, atLeastOnce()).save(prev);
    }

    @Test
    void testCreateAndSendForContract_NoMilestones_ThrowsBadRequest() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(10L)).thenReturn(List.of());

        assertThrows(ResponseStatusException.class, () -> signatureService.createAndSendForContract(10L));
    }

    @Test
    void testSign_TokenMismatch_ThrowsUnauthorized() {
        SignatureRequest req = new SignatureRequest();
        req.setId(1L);
        req.setStatus(SignatureRequestStatus.SENT);
        when(signatureRequestRepository.findById(1L)).thenReturn(Optional.of(req));

        SignatureSigner signer = new SignatureSigner();
        signer.setTokenHash("other-hash");
        when(signatureSignerRepository.findBySignatureRequestId(1L)).thenReturn(List.of(signer));

        SigningSignRequest signReq = new SigningSignRequest();
        signReq.setToken("wrong-token");
        signReq.setSignatureType(SignatureType.TYPED);
        signReq.setSignaturePayload("sig");

        assertThrows(ResponseStatusException.class, () -> signatureService.sign(1L, signReq, "1.1.1.1", "ua"));
    }

    @Test
    void testSign_WhenRequestCancelled_ThrowsGone() {
        SignatureRequest req = new SignatureRequest();
        req.setId(1L);
        req.setContractId(10L);
        req.setStatus(SignatureRequestStatus.CANCELLED);
        when(signatureRequestRepository.findById(1L)).thenReturn(Optional.of(req));

        SigningSignRequest signReq = new SigningSignRequest();
        signReq.setToken("tok");
        signReq.setSignatureType(SignatureType.TYPED);
        signReq.setSignaturePayload("sig");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                signatureService.sign(1L, signReq, "1.2.3.4", "ua");
            }
        });
        assertEquals(410, ex.getStatusCode().value());
        verify(signatureSignerRepository, never()).save(any());
    }

    @Test
    void testSign_WhenRequestCompleted_IsIdempotent() {
        SignatureRequest req = new SignatureRequest();
        req.setId(1L);
        req.setContractId(10L);
        req.setStatus(SignatureRequestStatus.COMPLETED);
        when(signatureRequestRepository.findById(1L)).thenReturn(Optional.of(req));

        SigningSignRequest signReq = new SigningSignRequest();
        signReq.setToken("tok");
        signReq.setSignatureType(SignatureType.TYPED);
        signReq.setSignaturePayload("sig");

        assertDoesNotThrow(() -> signatureService.sign(1L, signReq, "1.2.3.4", "ua"));
        verify(signatureSignerRepository, never()).save(any());
        verify(signatureRequestRepository, never()).save(any());
        verify(contractRepository, never()).save(any());
    }

    @Test
    void testSign_TokenExpired_ThrowsGone() {
        String token = "tok_123";

        SignatureRequest req = new SignatureRequest();
        req.setId(1L);
        req.setContractId(10L);
        req.setStatus(SignatureRequestStatus.SENT);
        req.setSnapshotHash("hash");
        when(signatureRequestRepository.findById(1L)).thenReturn(Optional.of(req));

        SignatureSigner signer = new SignatureSigner();
        signer.setSignatureRequestId(1L);
        signer.setRole(SignerRole.CLIENT);
        signer.setTokenHash(sha256Hex(token));
        signer.setStatus(SignatureSignerStatus.PENDING);
        signer.setTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(signatureSignerRepository.findBySignatureRequestId(1L)).thenReturn(List.of(signer));

        SigningSignRequest signReq = new SigningSignRequest();
        signReq.setToken(token);
        signReq.setSignatureType(SignatureType.TYPED);
        signReq.setSignaturePayload("sig");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                signatureService.sign(1L, signReq, "1.2.3.4", "ua");
            }
        });
        assertEquals(410, ex.getStatusCode().value());
        verify(signatureSignerRepository, never()).save(any());
    }

    @Test
    void testSign_SignerAlreadySigned_IsIdempotent() {
        String token = "tok_123";

        SignatureRequest req = new SignatureRequest();
        req.setId(1L);
        req.setContractId(10L);
        req.setStatus(SignatureRequestStatus.SENT);
        req.setSnapshotHash("hash");
        when(signatureRequestRepository.findById(1L)).thenReturn(Optional.of(req));

        SignatureSigner signer = new SignatureSigner();
        signer.setSignatureRequestId(1L);
        signer.setRole(SignerRole.CLIENT);
        signer.setTokenHash(sha256Hex(token));
        signer.setStatus(SignatureSignerStatus.SIGNED);
        when(signatureSignerRepository.findBySignatureRequestId(1L)).thenReturn(List.of(signer));

        SigningSignRequest signReq = new SigningSignRequest();
        signReq.setToken(token);
        signReq.setSignatureType(SignatureType.TYPED);
        signReq.setSignaturePayload("sig");

        assertDoesNotThrow(() -> signatureService.sign(1L, signReq, "1.2.3.4", "ua"));
        verify(signatureSignerRepository, never()).save(any());
        verify(signatureRequestRepository, never()).save(any());
        verify(contractRepository, never()).save(any());
    }

    @Test
    void testSign_AllSigned_CompletesRequestAndMovesContractToPendingPayment() {
        String token = "tok_123";

        SignatureRequest req = new SignatureRequest();
        req.setId(1L);
        req.setContractId(10L);
        req.setStatus(SignatureRequestStatus.SENT);
        req.setSnapshotHash("hash");
        when(signatureRequestRepository.findById(1L)).thenReturn(Optional.of(req));
        when(signatureRequestRepository.save(any(SignatureRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        SignatureSigner signerToSign = new SignatureSigner();
        signerToSign.setSignatureRequestId(1L);
        signerToSign.setRole(SignerRole.CLIENT);
        signerToSign.setTokenHash(sha256Hex(token));
        signerToSign.setStatus(SignatureSignerStatus.PENDING);

        SignatureSigner otherSigner = new SignatureSigner();
        otherSigner.setSignatureRequestId(1L);
        otherSigner.setRole(SignerRole.FREELANCER);
        otherSigner.setTokenHash(sha256Hex("tok_other"));
        otherSigner.setStatus(SignatureSignerStatus.SIGNED);

        when(signatureSignerRepository.findBySignatureRequestId(1L)).thenReturn(List.of(signerToSign, otherSigner));
        when(signatureSignerRepository.save(any(SignatureSigner.class))).thenAnswer(inv -> inv.getArgument(0));

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        contract.setDateSignature(null);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

        SigningSignRequest signReq = new SigningSignRequest();
        signReq.setToken(token);
        signReq.setSignatureType(SignatureType.TYPED);
        signReq.setSignaturePayload("sig");

        assertDoesNotThrow(() -> signatureService.sign(1L, signReq, "1.2.3.4", "ua"));

        assertEquals(SignatureRequestStatus.COMPLETED, req.getStatus());
        assertNotNull(req.getCompletedAt());
        assertEquals(SignatureSignerStatus.SIGNED, signerToSign.getStatus());
        assertNotNull(signerToSign.getSignedAt());
    }

    @Test
    void testCreateAndSendForContract_MissingSignerEmail_ThrowsBadRequest() {
        Contract contract = new Contract();
        contract.setId(10L);
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        tn.esprit.mscontractservicee.dto.UserDTO client = new tn.esprit.mscontractservicee.dto.UserDTO();
        client.setEmail(""); // Missing
        when(contractService.getClientInfo(10L)).thenReturn(client);
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(10L)).thenReturn(List.of(new Milestone()));

        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                signatureService.createAndSendForContract(10L);
            }
        });
    }

    @Test
    void testViewForToken_Expired_ThrowsGone() {
        String token = "tok";
        SignatureRequest req = new SignatureRequest();
        req.setId(1L);
        req.setStatus(SignatureRequestStatus.SENT);
        req.setSnapshotJson("{}");
        when(signatureRequestRepository.findById(1L)).thenReturn(Optional.of(req));

        SignatureSigner signer = new SignatureSigner();
        signer.setTokenHash(sha256Hex(token));
        signer.setTokenExpiresAt(LocalDateTime.now().minusHours(1));
        when(signatureSignerRepository.findBySignatureRequestId(1L)).thenReturn(List.of(signer));

        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                signatureService.viewForToken(1L, token);
            }
        });
    }

    @Test
    void testSha256Hex_InternalCheck() {
        String token = "test";
        String hash = sha256Hex(token);
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void testCreateAndSendForContract_NullContractId_ThrowsBadRequest() {
        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                signatureService.createAndSendForContract(null);
            }
        });
    }

    @Test
    void testCreateAndSendForContract_CancelsPreviousSentRequest_ThenFailsOnMilestones() {
        Contract contract = new Contract();
        contract.setId(5L);
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        when(contractRepository.findById(5L)).thenReturn(Optional.of(contract));

        // Existing SENT request that should be cancelled
        SignatureRequest prev = new SignatureRequest();
        prev.setId(99L);
        prev.setStatus(SignatureRequestStatus.SENT);
        when(signatureRequestRepository.findTopByContractIdOrderByIdDesc(5L))
                .thenReturn(Optional.of(prev));
        when(signatureRequestRepository.save(any())).thenReturn(prev);

        // Empty milestones → triggers the 400 after the cancellation logic
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(5L)).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                signatureService.createAndSendForContract(5L);
            }
        });
        assertEquals(400, ex.getStatusCode().value());
        // Verify the previous request was cancelled and saved
        verify(signatureRequestRepository, atLeastOnce()).save(any());
    }
    private Contract buildPendingSignatureContract() {
        Contract contract = new Contract();
        contract.setId(20L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        contract.setReference("CTR-SIGN-TEST");
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        contract.setVersion(1);
        return contract;
    }

    private void mockFullFlowSetup(Contract contract) {
        when(contractRepository.findById(20L)).thenReturn(Optional.of(contract));
        when(signatureRequestRepository.findTopByContractIdOrderByIdDesc(20L)).thenReturn(Optional.empty());

        Milestone m = new Milestone();
        m.setId(1L);
        m.setTitre("Milestone 1");
        when(milestoneRepository.findByContractIdOrderByOrdreAsc(20L)).thenReturn(List.of(m));

        SignatureRequest savedReq = new SignatureRequest();
        savedReq.setId(55L);
        savedReq.setStatus(SignatureRequestStatus.CREATED);
        when(signatureRequestRepository.save(any())).thenReturn(savedReq);

        UserDTO client = new UserDTO();
        client.setEmail("client@test.com");
        UserDTO freelancer = new UserDTO();
        freelancer.setEmail("freelancer@test.com");
        when(contractService.getClientInfo(20L)).thenReturn(client);
        when(contractService.getFreelancerInfo(20L)).thenReturn(freelancer);

        when(signatureSignerRepository.save(any())).thenReturn(new SignatureSigner());
    }

    @Test
    void testCreateAndSendForContract_ClientEmailFails_ReturnsCreatedStatus() {
        Contract contract = buildPendingSignatureContract();
        mockFullFlowSetup(contract);

        // Client email fails → covers line 144 (clientMailSent = false)
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService).sendSignatureRequestEmail(eq("client@test.com"), any(), any());
        doNothing()
                .when(emailService).sendSignatureRequestEmail(eq("freelancer@test.com"), any(), any());

        SignatureRequestCreateResponse res = signatureService.createAndSendForContract(20L);
        assertNotNull(res);
        assertFalse(res.isEmailsSent()); // covers lines 161-162
    }

    @Test
    void testCreateAndSendForContract_FreelancerEmailFails_ReturnsCreatedStatus() {
        Contract contract = buildPendingSignatureContract();
        mockFullFlowSetup(contract);

        // Freelancer email fails → covers line 150 (freelancerMailSent = false)
        doNothing()
                .when(emailService).sendSignatureRequestEmail(eq("client@test.com"), any(), any());
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService).sendSignatureRequestEmail(eq("freelancer@test.com"), any(), any());

        SignatureRequestCreateResponse res = signatureService.createAndSendForContract(20L);
        assertNotNull(res);
        assertFalse(res.isEmailsSent());
    }

    @Test
    void testCreateAndSendForContract_BothEmailsSucceed_ReturnsEmailsSentTrue() {
        Contract contract = buildPendingSignatureContract();
        mockFullFlowSetup(contract);

        // Both emails succeed → covers lines 156-157 (req.setStatus(SENT), req.setSentAt)
        doNothing().when(emailService).sendSignatureRequestEmail(any(), any(), any());

        SignatureRequestCreateResponse res = signatureService.createAndSendForContract(20L);
        assertNotNull(res);
        assertTrue(res.isEmailsSent());
    }

    @Test
    void testViewForToken_NotFound_Throws() {
        when(signatureRequestRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> signatureService.viewForToken(999L, "token"));
    }

    @Test
    void testFromJson_Failure_ThrowsInternalServerError() {
        SignatureRequest req = new SignatureRequest();
        req.setId(55L);
        req.setSnapshotJson("invalid-json");
        when(signatureRequestRepository.findById(55L)).thenReturn(Optional.of(req));
        
        SignatureSigner signer = new SignatureSigner();
        signer.setTokenHash(sha256Hex("valid-token"));
        when(signatureSignerRepository.findBySignatureRequestId(55L)).thenReturn(List.of(signer));

        assertThrows(ResponseStatusException.class, () -> signatureService.viewForToken(55L, "valid-token"));
    }
}
