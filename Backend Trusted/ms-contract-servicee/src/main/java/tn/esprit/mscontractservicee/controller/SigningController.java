package tn.esprit.mscontractservicee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.mscontractservicee.dto.signing.SigningRequestViewResponse;
import tn.esprit.mscontractservicee.dto.signing.SigningSignRequest;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;
import tn.esprit.mscontractservicee.repository.SignatureSignerRepository;
import tn.esprit.mscontractservicee.service.IContractService;
import tn.esprit.mscontractservicee.service.ISignatureRequestService;
import tn.esprit.mscontractservicee.service.document.ContractDocumentService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/signing")
@RequiredArgsConstructor
@Tag(name = "Signing", description = "Public endpoints for in-app signature links")
public class SigningController {

    private final ISignatureRequestService signatureRequestService;
    private final IContractService contractService;
    private final MilestoneRepository milestoneRepository;
    private final ContractDocumentService contractDocumentService;
    private final SignatureSignerRepository signatureSignerRepository;

    @GetMapping("/requests/{id}")
    @Operation(summary = "View signing request (token required)")
    public ResponseEntity<SigningRequestViewResponse> view(@PathVariable Long id,
                                                           @RequestParam String token) {
        return ResponseEntity.ok(signatureRequestService.viewForToken(id, token));
    }

    @GetMapping(value = "/requests/{id}/document", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download signing PDF (token required)")
    public ResponseEntity<byte[]> document(@PathVariable Long id,
                                           @RequestParam String token) {
        SigningRequestViewResponse view = signatureRequestService.viewForToken(id, token);
        Contract contract = contractService.findById(view.getContractId()).orElse(null);
        if (contract == null) {
            return ResponseEntity.notFound().build();
        }
        var milestones = milestoneRepository.findByContractIdOrderByOrdreAsc(contract.getId());
        var signers = signatureSignerRepository.findBySignatureRequestId(view.getSignatureRequestId());
        byte[] pdf = contractDocumentService.generateContractPdf(contract, milestones, signers);
        String filename = (contract.getReference() != null ? contract.getReference() : ("contract-" + contract.getId())) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/requests/{id}/sign")
    @Operation(summary = "Sign the contract snapshot (token in body)")
    public ResponseEntity<Map<String, Object>> sign(@PathVariable Long id,
                                                    @RequestBody SigningSignRequest req,
                                                    HttpServletRequest httpReq) {
        String ip = httpReq != null ? httpReq.getRemoteAddr() : null;
        String ua = httpReq != null ? httpReq.getHeader("User-Agent") : null;
        signatureRequestService.sign(id, req, ip, ua);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
