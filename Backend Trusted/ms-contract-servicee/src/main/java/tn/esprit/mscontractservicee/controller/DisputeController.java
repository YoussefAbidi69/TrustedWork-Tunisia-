package tn.esprit.mscontractservicee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.dto.dispute.DisputeAssignRequest;
import tn.esprit.mscontractservicee.dto.dispute.DisputeCreateRequest;
import tn.esprit.mscontractservicee.dto.dispute.DisputeEvidenceResponse;
import tn.esprit.mscontractservicee.dto.dispute.DisputeResolveRequest;
import tn.esprit.mscontractservicee.dto.dispute.DisputeRespondRequest;
import tn.esprit.mscontractservicee.dto.dispute.DisputeAiRecommendation;
import tn.esprit.mscontractservicee.entity.Dispute;
import tn.esprit.mscontractservicee.service.IDisputeAiService;
import tn.esprit.mscontractservicee.service.IDisputeEvidenceService;
import tn.esprit.mscontractservicee.service.IDisputeService;

import java.util.List;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
@Tag(name = "Dispute", description = "API pour la gestion des litiges")
public class DisputeController {

    private static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    private final IDisputeService disputeService;
    private final IDisputeEvidenceService disputeEvidenceService;
    private final IDisputeAiService disputeAiService;

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

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().contains(ROLE_ADMIN);
    }

    @PostMapping
    @Operation(summary = "Ouvrir un litige (CLIENT/FREELANCER)")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<Dispute> open(Authentication authentication,
                                        @RequestBody DisputeCreateRequest request) {
        Dispute saved = disputeService.openDispute(currentCin(authentication), request);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/ai-analysis")
    @Operation(summary = "Analyse AI du litige — recommandation Gemini à la volée (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisputeAiRecommendation> aiAnalysis(Authentication authentication,
                                                              @PathVariable Long id) {
        DisputeAiRecommendation recommendation = disputeAiService.analyze(id, currentCin(authentication));
        return ResponseEntity.ok(recommendation);
    }

    @PostMapping("/{id}/respond")
    @Operation(summary = "Repondre au litige (defendeur)")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<Dispute> respond(Authentication authentication,
                                           @PathVariable Long id,
                                           @RequestBody DisputeRespondRequest request) {
        Dispute updated = disputeService.respond(id, currentCin(authentication), isAdmin(authentication), request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assigner un arbitre (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Dispute> assign(Authentication authentication,
                                          @PathVariable Long id,
                                          @RequestBody(required = false) DisputeAssignRequest request) {
        Dispute updated = disputeService.assign(id, currentCin(authentication), request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resoudre le litige (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Dispute> resolve(Authentication authentication,
                                           @PathVariable Long id,
                                           @RequestBody DisputeResolveRequest request) {
        Dispute updated = disputeService.resolve(id, currentCin(authentication), request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un litige par ID")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<Dispute> getById(Authentication authentication,
                                           @PathVariable Long id) {
        Dispute dispute = disputeService.getByIdForUser(id, currentCin(authentication), isAdmin(authentication));
        return ResponseEntity.ok(dispute);
    }

    @GetMapping
    @Operation(summary = "Lister les litiges d'un contrat")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<List<Dispute>> listByContract(Authentication authentication,
                                                        @RequestParam Long contractId) {
        List<Dispute> disputes = disputeService.listByContractForUser(contractId, currentCin(authentication), isAdmin(authentication));
        return ResponseEntity.ok(disputes);
    }

    @GetMapping("/milestone/{milestoneId}")
    @Operation(summary = "Lister les litiges d'un jalon")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<List<Dispute>> listByMilestone(Authentication authentication,
                                                         @PathVariable Long milestoneId) {
        List<Dispute> disputes = disputeService.listByMilestoneForUser(milestoneId, currentCin(authentication), isAdmin(authentication));
        return ResponseEntity.ok(disputes);
    }

    @PostMapping(value = "/{id}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader une piece jointe (CLIENT/FREELANCER/ADMIN)")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<DisputeEvidenceResponse> uploadEvidence(Authentication authentication,
                                                                  @PathVariable Long id,
                                                                  @RequestParam("file") MultipartFile file) {
        DisputeEvidenceResponse saved = disputeEvidenceService.uploadEvidence(id, currentCin(authentication), isAdmin(authentication), file);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/evidence")
    @Operation(summary = "Lister les pieces jointes d'un litige")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<List<DisputeEvidenceResponse>> listEvidence(Authentication authentication,
                                                                      @PathVariable Long id) {
        List<DisputeEvidenceResponse> list = disputeEvidenceService.listEvidence(id, currentCin(authentication), isAdmin(authentication));
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}/evidence/{evidenceId}/download")
    @Operation(summary = "Telecharger une piece jointe d'un litige")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<byte[]> downloadEvidence(Authentication authentication,
                                                   @PathVariable Long id,
                                                   @PathVariable Long evidenceId) {
        var dl = disputeEvidenceService.downloadEvidence(id, evidenceId, currentCin(authentication), isAdmin(authentication));
        DisputeEvidenceResponse meta = dl.meta();
        String filename = meta.getOriginalFilename() != null ? meta.getOriginalFilename() : ("evidence-" + evidenceId);
        MediaType mt = MediaType.APPLICATION_OCTET_STREAM;
        if (meta.getContentType() != null && !meta.getContentType().isBlank()) {
            try {
                mt = MediaType.parseMediaType(meta.getContentType());
            } catch (Exception ignored) {
                mt = MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        // Read all bytes explicitly to avoid any proxy/converter issues; files are capped by multipart limits.
        byte[] bytes;
        try {
            if (dl.resource() instanceof org.springframework.core.io.FileSystemResource fsr) {
                bytes = java.nio.file.Files.readAllBytes(fsr.getFile().toPath());
            } else {
                try (var in = dl.resource().getInputStream()) {
                    bytes = in.readAllBytes();
                }
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read evidence file");
        }

        String sha256Hex;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(bytes);
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            sha256Hex = sb.toString();
        } catch (Exception e) {
            sha256Hex = null;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header("X-File-SHA256", sha256Hex != null ? sha256Hex : "")
                .contentType(mt)
                .contentLength(bytes.length)
                .body(bytes);
    }
}
