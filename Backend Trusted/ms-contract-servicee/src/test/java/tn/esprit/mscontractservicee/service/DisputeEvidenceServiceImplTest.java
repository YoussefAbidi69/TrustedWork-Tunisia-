package tn.esprit.mscontractservicee.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.dto.dispute.DisputeEvidenceResponse;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Dispute;
import tn.esprit.mscontractservicee.entity.DisputeEvidence;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.DisputeEvidenceRepository;
import tn.esprit.mscontractservicee.repository.DisputeRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeEvidenceServiceImplTest {

    @Mock
    private DisputeEvidenceRepository evidenceRepository;
    @Mock
    private DisputeRepository disputeRepository;
    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private DisputeEvidenceServiceImpl evidenceService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(evidenceService, "uploadDir", tempDir.toString());
    }

    @Test
    void testUploadEvidence_Success() {
        Long disputeId = 1L;
        Long authenticatedCin = 100L;
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        Dispute dispute = new Dispute();
        dispute.setId(disputeId);
        dispute.setContractId(10L);
        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(100L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        when(evidenceRepository.save(any(DisputeEvidence.class))).thenAnswer(i -> {
            DisputeEvidence e = i.getArgument(0);
            e.setId(1L);
            return e;
        });

        DisputeEvidenceResponse res = evidenceService.uploadEvidence(disputeId, authenticatedCin, false, file);

        assertNotNull(res);
        assertEquals("test.txt", res.getOriginalFilename());
        assertEquals(100L, res.getUploaderCin());
        verify(evidenceRepository).save(any(DisputeEvidence.class));
    }

    @Test
    void testUploadEvidence_NotParticipant_ThrowsForbidden() {
        Long disputeId = 1L;
        Long authenticatedCin = 999L; // Intrus
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        Dispute dispute = new Dispute();
        dispute.setId(disputeId);
        dispute.setContractId(10L);
        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        assertThrows(ResponseStatusException.class, () -> 
            evidenceService.uploadEvidence(disputeId, authenticatedCin, false, file));
    }

    @Test
    void testListEvidence_Success() {
        Long disputeId = 1L;
        Long authenticatedCin = 100L;

        Dispute dispute = new Dispute();
        dispute.setId(disputeId);
        dispute.setContractId(10L);
        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(100L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        DisputeEvidence evidence = new DisputeEvidence();
        evidence.setId(1L);
        evidence.setDisputeId(disputeId);
        when(evidenceRepository.findByDisputeIdOrderByCreatedAtDesc(disputeId)).thenReturn(List.of(evidence));

        List<DisputeEvidenceResponse> res = evidenceService.listEvidence(disputeId, authenticatedCin, false);

        assertEquals(1, res.size());
        assertEquals(1L, res.get(0).getId());
    }

    @Test
    void testDownloadEvidence_NotFound_ThrowsNotFound() {
        Long disputeId = 1L;
        Long evidenceId = 50L;
        Long authenticatedCin = 100L;

        Dispute dispute = new Dispute();
        dispute.setId(disputeId);
        dispute.setContractId(10L);
        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setClientCin(100L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        when(evidenceRepository.findByIdAndDisputeId(evidenceId, disputeId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> 
            evidenceService.downloadEvidence(disputeId, evidenceId, authenticatedCin, false));
    }
}
