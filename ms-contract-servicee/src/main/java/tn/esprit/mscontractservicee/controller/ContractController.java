package tn.esprit.mscontractservicee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.SignatureRequest;
import tn.esprit.mscontractservicee.entity.SignatureSigner;
import tn.esprit.mscontractservicee.dto.signing.SignatureRequestCreateResponse;
import tn.esprit.mscontractservicee.dto.signing.ContractSignatureStatusResponse;
import tn.esprit.mscontractservicee.dto.ContractFinancialMetricsResponse;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.enums.SignatureRequestStatus;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;
import tn.esprit.mscontractservicee.repository.SignatureRequestRepository;
import tn.esprit.mscontractservicee.repository.SignatureSignerRepository;
import tn.esprit.mscontractservicee.service.IContractService;
import tn.esprit.mscontractservicee.service.ISignatureRequestService;
import tn.esprit.mscontractservicee.service.ContractTotalService;
import tn.esprit.mscontractservicee.service.document.ContractDocumentService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
@Tag(name = "Contract", description = "API pour la gestion des contrats")
public class ContractController {

    private static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
    private static final SimpleGrantedAuthority ROLE_CLIENT = new SimpleGrantedAuthority("ROLE_CLIENT");
    private static final SimpleGrantedAuthority ROLE_FREELANCER = new SimpleGrantedAuthority("ROLE_FREELANCER");

    private final IContractService contractService;
    private final ISignatureRequestService signatureRequestService;
    private final MilestoneRepository milestoneRepository;
    private final ContractDocumentService contractDocumentService;
    private final SignatureRequestRepository signatureRequestRepository;
    private final SignatureSignerRepository signatureSignerRepository;
    private final tn.esprit.mscontractservicee.service.IContractAiGenerationService contractAiService;
    private final ContractTotalService contractTotalService;

    private static boolean hasRole(Authentication authentication, SimpleGrantedAuthority role) {
        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().contains(role);
    }

    private static boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, ROLE_ADMIN);
    }

    private static Long currentCin(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated CIN");
        }
    }

    private static boolean isContractParticipant(Contract contract, Long cin, boolean admin) {
        if (contract == null || cin == null) {
            return false;
        }
        if (admin) {
            return true;
        }
        return (contract.getClientCin() != null && contract.getClientCin().equals(cin))
                || (contract.getFreelancerCin() != null && contract.getFreelancerCin().equals(cin));
    }

    @GetMapping("/test")
    @Operation(summary = "Test endpoint")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "ms-contract-service is running!");
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ai/generate")
    @Operation(summary = "Générer un brouillon de contrat via IA")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<tn.esprit.mscontractservicee.dto.ai.ContractAiResponse> generateContractDraft(
            @RequestBody tn.esprit.mscontractservicee.dto.ai.ContractAiPromptRequest request) {
        return ResponseEntity.ok(contractAiService.generateContractDraft(request));
    }

    @PostMapping
    @Operation(summary = "Créer un contrat")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<Contract> createContract(Authentication authentication,
                                                   @RequestBody Contract contract) {
        Contract saved = contractService.createContract(contract, currentCin(authentication));
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un contrat par ID")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<Contract> getContractById(Authentication authentication,
                                                    @PathVariable Long id) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);

        Contract contract = contractService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + id));

        if (!isContractParticipant(contract, cin, admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to access this contract");
        }

        if (!admin
                && hasRole(authentication, ROLE_FREELANCER)
                && contract.getFreelancerCin() != null
                && contract.getFreelancerCin().equals(cin)
                && contract.getDateSignature() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to access an unsigned contract");
        }

        return ResponseEntity.ok(contract);
    }

    @GetMapping("/{id}/wallet-ids")
    @Operation(summary = "Récupérer les wallet IDs liés au contrat")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<?> getContractWalletIds(Authentication authentication,
                                                  @PathVariable Long id) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);

        Contract contract = contractService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + id));

        if (!isContractParticipant(contract, cin, admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to access this contract");
        }

        if (!admin
                && hasRole(authentication, ROLE_FREELANCER)
                && contract.getFreelancerCin() != null
                && contract.getFreelancerCin().equals(cin)
                && contract.getDateSignature() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to access an unsigned contract");
        }

        return ResponseEntity.ok(contractService.getWalletIds(id));
    }

    @GetMapping("/{id}/financial-metrics")
    @Operation(summary = "Calculer le montant total depuis les jalons + detecter mismatch")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<ContractFinancialMetricsResponse> getContractFinancialMetrics(Authentication authentication,
                                                                                        @PathVariable Long id) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);

        Contract contract = contractService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + id));

        if (!isContractParticipant(contract, cin, admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to access this contract");
        }

        if (!admin
                && hasRole(authentication, ROLE_FREELANCER)
                && contract.getFreelancerCin() != null
                && contract.getFreelancerCin().equals(cin)
                && contract.getDateSignature() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to access an unsigned contract");
        }

        return ResponseEntity.ok(contractTotalService.getFinancialMetrics(id));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my contracts")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<Page<Contract>> getMyContracts(Authentication authentication,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        Long cin = currentCin(authentication);
        Pageable pageable = PageRequest.of(page, size);

        if (isAdmin(authentication)) {
            return ResponseEntity.ok(contractService.findAll(pageable));
        }
        if (hasRole(authentication, ROLE_CLIENT)) {
            return ResponseEntity.ok(contractService.findByClientCin(cin, pageable));
        }
        if (hasRole(authentication, ROLE_FREELANCER)) {
            return ResponseEntity.ok(contractService.findSignedByFreelancerCin(cin, pageable));
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role");
    }

    @GetMapping("/freelancer/{freelancerCin}/signed")
    @Operation(summary = "List signed contracts for a freelancer")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('ADMIN')")
    public ResponseEntity<Page<Contract>> getSignedContractsByFreelancer(Authentication authentication,
                                                                         @PathVariable Long freelancerCin,
                                                                         @RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        if (!isAdmin(authentication) && !freelancerCin.equals(currentCin(authentication))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to access freelancer contracts");
        }

        return ResponseEntity.ok(contractService.findSignedByFreelancerCin(freelancerCin, pageable));
    }

    @GetMapping
    @Operation(summary = "Récupérer tous les contrats (paginé)")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<Page<Contract>> getAllContracts(Authentication authentication,
                                                           @RequestParam(required = false) Long userCin,
                                                           @RequestParam(required = false) Long freelancerCin,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        Long cin = currentCin(authentication);
        Pageable pageable = PageRequest.of(page, size);

        if (isAdmin(authentication)) {
            if (userCin != null) {
                return ResponseEntity.ok(contractService.findByUserCin(userCin, pageable));
            }
            if (freelancerCin != null) {
                return ResponseEntity.ok(contractService.findSignedByFreelancerCin(freelancerCin, pageable));
            }
            return ResponseEntity.ok(contractService.findAll(pageable));
        }

        if (hasRole(authentication, ROLE_CLIENT)) {
            return ResponseEntity.ok(contractService.findByClientCin(cin, pageable));
        }
        if (hasRole(authentication, ROLE_FREELANCER)) {
            return ResponseEntity.ok(contractService.findSignedByFreelancerCin(cin, pageable));
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un contrat")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<Contract> updateContract(Authentication authentication,
                                                   @PathVariable Long id,
                                                   @RequestBody Contract contract) {
        Long cin = currentCin(authentication);
        Contract existing = contractService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + id));
        if (!isAdmin(authentication)
                && (existing.getClientCin() == null || !existing.getClientCin().equals(cin))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to update this contract");
        }
        return ResponseEntity.ok(contractService.updateContract(id, contract));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Changer le statut d'un contrat")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<Contract> updateContractStatus(Authentication authentication,
                                                         @PathVariable Long id,
                                                         @RequestParam ContractStatus status) {
        Long cin = currentCin(authentication);
        Contract existing = contractService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + id));
        if (!isAdmin(authentication)
                && (existing.getClientCin() == null || !existing.getClientCin().equals(cin))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to update this contract");
        }
        return ResponseEntity.ok(contractService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un contrat")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteContract(Authentication authentication,
                                               @PathVariable Long id) {
        Long cin = currentCin(authentication);
        Contract existing = contractService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + id));
        if (!isAdmin(authentication)
                && (existing.getClientCin() == null || !existing.getClientCin().equals(cin))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to delete this contract");
        }
        contractService.deleteContract(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/finalize")
    @Operation(summary = "Finaliser le contrat (verrouiller) avant signature")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<Contract> finalizeContract(Authentication authentication,
                                                     @PathVariable Long id) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);

        Contract existing = contractService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + id));
        if (!admin && (existing.getClientCin() == null || !existing.getClientCin().equals(cin))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to finalize this contract");
        }

        return ResponseEntity.ok(contractService.finalizeForSignature(id));
    }

    @PostMapping("/{id}/signature-requests")
    @Operation(summary = "Envoyer la demande de signature numerique (email client + freelancer)")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<SignatureRequestCreateResponse> createSignatureRequest(Authentication authentication,
                                                                                @PathVariable Long id) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);

        Contract existing = contractService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + id));
        if (!admin && (existing.getClientCin() == null || !existing.getClientCin().equals(cin))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to request signatures for this contract");
        }

        return ResponseEntity.ok(signatureRequestService.createAndSendForContract(id));
    }

    @GetMapping(value = "/{id}/document", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Telecharger le document PDF du contrat")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<byte[]> downloadContractDocument(Authentication authentication,
                                                           @PathVariable Long id) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);

        Contract contract = contractService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + id));
        if (!isContractParticipant(contract, cin, admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to access this contract document");
        }

        // Always include milestones in the document view.
        var milestones = milestoneRepository.findByContractIdOrderByOrdreAsc(id);
        SignatureRequest latestReq = signatureRequestRepository.findTopByContractIdOrderByIdDesc(id).orElse(null);
        List<SignatureSigner> signers = latestReq != null
                ? signatureSignerRepository.findBySignatureRequestId(latestReq.getId())
                : Collections.emptyList();
        byte[] pdf = contractDocumentService.generateContractPdf(contract, milestones, signers);

        String filename = (contract.getReference() != null ? contract.getReference() : ("contract-" + id)) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/signature")
    @Operation(summary = "Recuperer le statut de signature du contrat (derniere demande)")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<ContractSignatureStatusResponse> signatureStatus(Authentication authentication,
                                                                           @PathVariable Long id) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);

        Contract contract = contractService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + id));
        if (!isContractParticipant(contract, cin, admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to access this contract signature status");
        }

        SignatureRequest latestReq = signatureRequestRepository.findTopByContractIdOrderByIdDesc(id).orElse(null);
        List<SignatureSigner> signers = latestReq != null
                ? signatureSignerRepository.findBySignatureRequestId(latestReq.getId())
                : Collections.emptyList();

        boolean fullySigned = latestReq != null
                && latestReq.getStatus() == SignatureRequestStatus.COMPLETED;

        List<ContractSignatureStatusResponse.SignerStatus> signerDtos = signers.stream()
                .map(s -> ContractSignatureStatusResponse.SignerStatus.builder()
                        .role(s.getRole() != null ? s.getRole().name() : null)
                        .email(s.getSignerEmail())
                        .status(s.getStatus() != null ? s.getStatus().name() : null)
                        .signedAt(s.getSignedAt())
                        .build())
                .toList();

        ContractSignatureStatusResponse res = ContractSignatureStatusResponse.builder()
                .contractId(contract.getId())
                .contractStatus(contract.getStatus() != null ? contract.getStatus().name() : null)
                .contractSignedAt(contract.getDateSignature())
                .signatureRequestId(latestReq != null ? latestReq.getId() : null)
                .signatureRequestStatus(latestReq != null && latestReq.getStatus() != null ? latestReq.getStatus().name() : null)
                .signatureRequestCreatedAt(latestReq != null ? latestReq.getCreatedAt() : null)
                .signatureRequestCompletedAt(latestReq != null ? latestReq.getCompletedAt() : null)
                .fullySigned(fullySigned)
                .signers(signerDtos)
                .build();

        return ResponseEntity.ok(res);
    }
}
