package tn.esprit.mscontractservicee.service.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.entity.SignatureSigner;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.enums.SignerRole;
import tn.esprit.mscontractservicee.enums.SignatureSignerStatus;
import tn.esprit.mscontractservicee.enums.SignatureType;
import tn.esprit.mscontractservicee.feign.UserServiceClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfBoxContractDocumentServiceTest {

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private PdfBoxContractDocumentService pdfService;

    @Test
    void testGenerateContractPdf_NullContract_Throws() {
        List<Milestone> milestones = Collections.emptyList();
        List<SignatureSigner> signers = Collections.emptyList();
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                pdfService.generateContractPdf(null, milestones, signers);
            }
        });
    }

    @Test
    void testGenerateContractPdf() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setReference("REF-123");
        contract.setProjectTitle("Test Contract");
        contract.setDescription("Test Description");
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setStatus(ContractStatus.ACTIVE);

        Milestone milestone = new Milestone();
        milestone.setTitre("Milestone 1");
        milestone.setMontant(new BigDecimal("500"));

        List<Milestone> milestones = List.of(milestone);
        List<SignatureSigner> signers = Collections.emptyList();

        byte[] pdf = pdfService.generateContractPdf(contract, milestones, signers);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testGenerateContractPdf_WithSignedSignersAndUserFetchFallback() {
        // Force fetchUserByCin() to execute and exercise the fallback branch.
        tn.esprit.mscontractservicee.dto.UserDTO dto = new tn.esprit.mscontractservicee.dto.UserDTO();
        dto.setFirstName("A");
        dto.setLastName("B");
        when(userServiceClient.getUserByCin(anyLong())).thenThrow(new RuntimeException("primary down"));
        when(userServiceClient.getUserByCinFromKycStatus(anyLong())).thenReturn(dto);

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setReference("REF-123");
        contract.setProjectTitle("Test Contract");
        contract.setDescription("Test Description");
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);

        SignatureSigner clientSigner = new SignatureSigner();
        clientSigner.setRole(SignerRole.CLIENT);
        clientSigner.setSignerEmail("client@test.com");
        clientSigner.setStatus(SignatureSignerStatus.SIGNED);
        clientSigner.setSignedAt(LocalDateTime.now());
        clientSigner.setSignatureType(SignatureType.TYPED);
        clientSigner.setSignaturePayload("Client Signature");

        // 1x1 PNG
        String pngB64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8Xw8AAoMBgShp4QAAAABJRU5ErkJggg==";
        SignatureSigner freelancerSigner = new SignatureSigner();
        freelancerSigner.setRole(SignerRole.FREELANCER);
        freelancerSigner.setSignerEmail("freelancer@test.com");
        freelancerSigner.setStatus(SignatureSignerStatus.SIGNED);
        freelancerSigner.setSignedAt(LocalDateTime.now());
        freelancerSigner.setSignatureType(SignatureType.DRAWN);
        freelancerSigner.setSignaturePayload(pngB64);

        byte[] pdf = pdfService.generateContractPdf(contract, List.of(), List.of(clientSigner, freelancerSigner));

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testGenerateContractPdf_UserFetchPrimaryAndFallbackFail_ReturnsPdf() {
        when(userServiceClient.getUserByCin(anyLong())).thenThrow(new RuntimeException("primary down"));
        when(userServiceClient.getUserByCinFromKycStatus(anyLong())).thenThrow(new RuntimeException("fallback down"));

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setReference("REF-123");
        contract.setProjectTitle("Test Contract");
        contract.setDescription("Test Description");
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);

        byte[] pdf = pdfService.generateContractPdf(contract, List.of(), List.of());

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testGenerateContractPdf_InvalidDrawnSignaturePayload_FallsBackToTyped() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setReference("REF-123");
        contract.setProjectTitle("Test Contract");
        contract.setDescription("Test Description");
        contract.setMontantTotal(new BigDecimal("1000"));
        contract.setStatus(ContractStatus.ACTIVE);

        SignatureSigner s1 = new SignatureSigner();
        s1.setRole(SignerRole.CLIENT);
        s1.setSignerEmail("client@test.com");
        s1.setStatus(SignatureSignerStatus.SIGNED);
        s1.setSignedAt(LocalDateTime.now());
        s1.setSignatureType(SignatureType.DRAWN);
        s1.setSignaturePayload("data:image/png;base64,not_base64");

        SignatureSigner s2 = new SignatureSigner();
        s2.setRole(SignerRole.FREELANCER);
        s2.setSignerEmail("freelancer@test.com");
        s2.setStatus(SignatureSignerStatus.SIGNED);
        s2.setSignedAt(LocalDateTime.now());
        s2.setSignatureType(SignatureType.DRAWN);
        s2.setSignaturePayload("   ");

        byte[] pdf = pdfService.generateContractPdf(contract, List.of(), List.of(s1, s2));

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testGenerateContractPdf_WithLongDescription_TriggersWrapping() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setReference("REF");
        StringBuilder longDesc = new StringBuilder();
        for (int i = 0; i < 20; i++) longDesc.append("Wrapping logic test. ");
        contract.setDescription(longDesc.toString());
        contract.setMontantTotal(new BigDecimal("1000"));

        byte[] pdf = pdfService.generateContractPdf(contract, List.of(), List.of());
        assertNotNull(pdf);
    }

    @Test
    void testGenerateContractPdf_MultipleMilestones_TriggersTable() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setReference("REF");
        contract.setMontantTotal(new BigDecimal("1000"));

        Milestone m1 = new Milestone();
        m1.setTitre("M1");
        m1.setMontant(new BigDecimal("500"));
        Milestone m2 = new Milestone();
        m2.setTitre("M2");
        m2.setMontant(new BigDecimal("500"));

        byte[] pdf = pdfService.generateContractPdf(contract, List.of(m1, m2), List.of());
        assertNotNull(pdf);
    }
}
