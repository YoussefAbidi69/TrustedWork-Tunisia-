package tn.esprit.mscontractservicee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.dto.DeliveryProofResponse;
import tn.esprit.mscontractservicee.dto.DeliveryProofSubmitRequest;
import tn.esprit.mscontractservicee.dto.milestone.MilestoneCreateRequest;
import tn.esprit.mscontractservicee.dto.milestone.MilestoneResponse;
import tn.esprit.mscontractservicee.dto.milestone.MilestoneUpdateRequest;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.enums.MilestoneStatus;
import tn.esprit.mscontractservicee.service.IContractService;
import tn.esprit.mscontractservicee.service.IDeliveryProofService;
import tn.esprit.mscontractservicee.service.IMilestoneService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/milestones")
@RequiredArgsConstructor
@Tag(name = "Milestone", description = "API pour la gestion des jalons")
public class MilestoneController {

    private final IMilestoneService milestoneService;
    private final IContractService contractService;
    private final IDeliveryProofService deliveryProofService;
    private final tn.esprit.mscontractservicee.service.IContractAiGenerationService contractAiService;

    @Value("${milestone.submission.requireDeliveryProof:false}")
    private boolean requireDeliveryProofOnSubmit;

    @Value("${milestone.approval.requireDeliveryProof:false}")
    private boolean requireDeliveryProofOnApprove;

    private static final String MILESTONE_NOT_FOUND_MSG = "Milestone not found with id: ";

    private static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
    private static final SimpleGrantedAuthority ROLE_CLIENT = new SimpleGrantedAuthority("ROLE_CLIENT");
    private static final SimpleGrantedAuthority ROLE_FREELANCER = new SimpleGrantedAuthority("ROLE_FREELANCER");

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

    private Contract requireContract(Long contractId) {
        if (contractId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contractId is required");
        }
        return contractService.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + contractId));
    }

    private void requireSignedContract(Contract contract) {
        if (contract.getDateSignature() == null || contract.getStatus() != ContractStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contract is not signed/active yet");
        }
    }

    @GetMapping("/test")
    @Operation(summary = "Test endpoint")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Milestone service is running!");
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ai/generate")
    @Operation(summary = "Générer un brouillon de jalon via IA")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<tn.esprit.mscontractservicee.dto.ai.MilestoneAiResponse> generateMilestoneDraft(
            @RequestBody tn.esprit.mscontractservicee.dto.ai.MilestoneAiPromptRequest request) {
        return ResponseEntity.ok(contractAiService.generateMilestoneDraft(request));
    }

    @PostMapping
    @Operation(summary = "Creer un jalon")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<MilestoneResponse> createMilestone(
            Authentication authentication,
            @RequestBody MilestoneCreateRequest milestone) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);

        Contract contract = requireContract(milestone.getContractId());
        if (!admin && (contract.getClientCin() == null || !contract.getClientCin().equals(cin))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to create milestones for this contract");
        }
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Milestones can only be created while contract is DRAFT. Current status: " + contract.getStatus());
        }

        Milestone saved = milestoneService.createMilestone(toEntity(milestone));
        return new ResponseEntity<>(toResponse(saved), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un jalon par ID")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<Milestone> getMilestoneById(
            Authentication authentication,
            @PathVariable Long id) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);
        Milestone milestone = milestoneService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));

        Contract contract = requireContract(milestone.getContractId());
        if (!isContractParticipant(contract, cin, admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this milestone");
        }

        return ResponseEntity.ok(milestone);
    }

    @GetMapping("/{id}/delivery-proof")
    @Operation(summary = "Recuperer la preuve de livraison (DeliveryProof) d'un jalon")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<DeliveryProofResponse> getDeliveryProofForMilestone(
            Authentication authentication,
            @PathVariable Long id) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);
        Milestone milestone = milestoneService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));

        Contract contract = requireContract(milestone.getContractId());
        if (!isContractParticipant(contract, cin, admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this delivery proof");
        }

        return deliveryProofService.findForMilestone(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "DeliveryProof not found for milestone id: " + id));
    }

    @GetMapping
    @Operation(summary = "Recuperer tous les jalons (pagine) [ADMIN]")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Milestone>> getAllMilestones(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(milestoneService.findAll(pageable));
    }

    @GetMapping("/me")
    @Operation(summary = "Recuperer mes jalons (CLIENT/FREELANCER)")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER')")
    public ResponseEntity<List<Milestone>> getMyMilestones(
            Authentication authentication) {
        Long cin = currentCin(authentication);
        if (hasRole(authentication, ROLE_CLIENT)) {
            return ResponseEntity.ok(milestoneService.findForClientCin(cin));
        }
        if (hasRole(authentication, ROLE_FREELANCER)) {
            return ResponseEntity.ok(milestoneService.findForSignedFreelancerCin(cin));
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un jalon")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<MilestoneResponse> updateMilestone(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody MilestoneUpdateRequest milestone) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);

        Milestone existing = milestoneService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));
        Contract contract = requireContract(existing.getContractId());

        if (!admin && (contract.getClientCin() == null || !contract.getClientCin().equals(cin))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to update this milestone");
        }
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Milestones can only be updated while contract is DRAFT. Current status: " + contract.getStatus());
        }

        return ResponseEntity.ok(toResponse(milestoneService.updateMilestone(id, toEntity(milestone, existing.getContractId()))));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Changer le statut d'un jalon [ADMIN]")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Milestone> updateMilestoneStatus(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam MilestoneStatus status) {
        return ResponseEntity.ok(milestoneService.updateStatus(id, status));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Demarrer un jalon")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('ADMIN')")
    public ResponseEntity<Milestone> startMilestone(
            Authentication authentication,
            @PathVariable Long id) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);

        Milestone milestone = milestoneService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));
        Contract contract = requireContract(milestone.getContractId());
        requireSignedContract(contract);

        if (!admin && (contract.getFreelancerCin() == null || !contract.getFreelancerCin().equals(cin))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to start milestones for this contract");
        }

        return ResponseEntity.ok(milestoneService.startMilestone(id));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Soumettre un jalon (et optionnellement sa preuve de livraison)")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('ADMIN')")
    public ResponseEntity<Milestone> submitMilestone(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody(required = false) DeliveryProofSubmitRequest deliveryProof) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);

        Milestone milestone = milestoneService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));
        Contract contract = requireContract(milestone.getContractId());
        requireSignedContract(contract);

        if (!admin && (contract.getFreelancerCin() == null || !contract.getFreelancerCin().equals(cin))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to submit milestones for this contract");
        }

        if (deliveryProof == null) {
            if (requireDeliveryProofOnSubmit) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deliveryProof payload is required");
            }
            return ResponseEntity.ok(milestoneService.submitMilestone(id));
        }

        return ResponseEntity.ok(milestoneService.submitMilestoneWithProof(id, deliveryProof));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approuver un jalon")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<Milestone> approveMilestone(
            Authentication authentication,
            @PathVariable Long id) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);

        Milestone milestone = milestoneService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));
        Contract contract = requireContract(milestone.getContractId());
        requireSignedContract(contract);

        if (!admin && (contract.getClientCin() == null || !contract.getClientCin().equals(cin))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to approve milestones for this contract");
        }

        if (requireDeliveryProofOnApprove && deliveryProofService.findForMilestone(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DeliveryProof is required before approval");
        }

        return ResponseEntity.ok(milestoneService.approveMilestone(id, cin));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Rejeter un jalon avec une raison (et une nouvelle deadline optionnelle)")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<Milestone> rejectMilestone(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDeadline) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);

        Milestone milestone = milestoneService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));
        Contract contract = requireContract(milestone.getContractId());
        requireSignedContract(contract);

        if (!admin && (contract.getClientCin() == null || !contract.getClientCin().equals(cin))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to reject milestones for this contract");
        }

        Milestone rejected = milestoneService.rejectMilestone(id, reason, newDeadline);
        return ResponseEntity.ok(rejected);
    }

    @PatchMapping("/{id}/deadline")
    @Operation(summary = "Modifier uniquement la deadline d'un jalon rejete (REJECTED) [CLIENT/ADMIN]")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<Milestone> updateRejectedMilestoneDeadline(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDeadline) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);

        Milestone milestone = milestoneService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));
        Contract contract = requireContract(milestone.getContractId());
        requireSignedContract(contract);

        if (!admin && (contract.getClientCin() == null || !contract.getClientCin().equals(cin))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to update milestone deadline for this contract");
        }

        return ResponseEntity.ok(milestoneService.updateRejectedMilestoneDeadline(id, newDeadline));
    }

    @PostMapping("/{id}/auto-approve")
    @Operation(summary = "Auto-approuver un jalon (SLA) [ADMIN]")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Milestone> autoApproveMilestone(
            Authentication authentication,
            @PathVariable Long id) {
        if (requireDeliveryProofOnApprove && deliveryProofService.findForMilestone(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DeliveryProof is required before approval");
        }
        return ResponseEntity.ok(milestoneService.autoApproveMilestone(id, currentCin(authentication)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un jalon")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMilestone(
            Authentication authentication,
            @PathVariable Long id) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);
        Milestone milestone = milestoneService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));
        Contract contract = requireContract(milestone.getContractId());

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Milestones can only be deleted while contract is DRAFT. Current status: " + contract.getStatus());
        }

        if (!admin) {
            if (contract.getClientCin() == null || !contract.getClientCin().equals(cin)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to delete this milestone");
            }
            if (milestone.getStatus() != MilestoneStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PENDING milestones can be deleted");
            }
        }

        milestoneService.deleteMilestone(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/contract/{contractId}")
    @Operation(summary = "Recuperer tous les jalons d'un contrat")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<List<Milestone>> getMilestonesByContract(
            Authentication authentication,
            @PathVariable Long contractId) {
        Long cin = currentCin(authentication);
        boolean admin = isAdmin(authentication);
        Contract contract = requireContract(contractId);

        if (!isContractParticipant(contract, cin, admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access these milestones");
        }

        List<Milestone> milestones = milestoneService.findByContractId(contractId);
        return ResponseEntity.ok(milestones);
    }

    private static MilestoneResponse toResponse(Milestone milestone) {
        if (milestone == null) {
            return null;
        }
        return MilestoneResponse.builder()
                .id(milestone.getId())
                .contractId(milestone.getContractId())
                .ordre(milestone.getOrdre())
                .titre(milestone.getTitre())
                .description(milestone.getDescription())
                .montant(milestone.getMontant())
                .deadline(milestone.getDeadline())
                .startedAt(milestone.getStartedAt())
                .submittedAt(milestone.getSubmittedAt())
                .validatedAt(milestone.getValidatedAt())
                .status(milestone.getStatus())
                .rejectionReason(milestone.getRejectionReason())
                .build();
    }

    private static Milestone toEntity(MilestoneCreateRequest req) {
        if (req == null) {
            return null;
        }
        Milestone m = new Milestone();
        m.setContractId(req.getContractId());
        m.setOrdre(req.getOrdre());
        m.setTitre(req.getTitre());
        m.setDescription(req.getDescription());
        m.setMontant(req.getMontant());
        m.setDeadline(req.getDeadline());
        return m;
    }

    private static Milestone toEntity(MilestoneUpdateRequest req, Long contractId) {
        if (req == null) {
            return null;
        }
        Milestone m = new Milestone();
        m.setContractId(contractId);
        m.setTitre(req.getTitre());
        m.setDescription(req.getDescription());
        m.setMontant(req.getMontant());
        m.setDeadline(req.getDeadline());
        return m;
    }
}
