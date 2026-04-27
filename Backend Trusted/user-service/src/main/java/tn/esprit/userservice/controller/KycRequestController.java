package tn.esprit.userservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.userservice.dto.KycRequestDTO;
import tn.esprit.userservice.dto.KycReviewRequest;
import tn.esprit.userservice.dto.KycSubmitRequest;
import tn.esprit.userservice.service.IKycRequestService;

import tn.esprit.userservice.service.CloudinaryService;

import java.util.List;

@RestController
@RequestMapping("/kyc/requests")
@RequiredArgsConstructor
@Tag(name = "KYC Requests", description = "Gestion avancée KYC")
public class KycRequestController {

    private final IKycRequestService kycRequestService;
    private final CloudinaryService   cloudinaryService;

    @PostMapping(value = "/submit/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<KycRequestDTO> submitKyc(
            @PathVariable Long userId,
            @RequestParam("cinNumber") String cinNumber,
            @RequestPart(value = "cinDocument", required = false) MultipartFile cinDocument,
            @RequestPart(value = "selfie",      required = false) MultipartFile selfie,
            @RequestPart(value = "diploma",     required = false) MultipartFile diploma
    ) {
        if (cinNumber == null || cinNumber.isBlank()) {
            throw new IllegalArgumentException("Le numéro CIN est obligatoire");
        }

        boolean hasCin     = cinDocument != null && !cinDocument.isEmpty();
        boolean hasSelfie  = selfie      != null && !selfie.isEmpty();
        boolean hasDiploma = diploma     != null && !diploma.isEmpty();

        // Soumission complète : CIN obligatoire (selfie optionnel)
        if (!hasCin && !hasDiploma) {
            throw new IllegalArgumentException("Le document CIN est obligatoire");
        }

        // Diplôme seul (APPROVED) — au moins un fichier requis
        if (!hasCin && !hasSelfie && !hasDiploma) {
            throw new IllegalArgumentException("Au moins un document est requis");
        }

        // Upload vers Cloudinary
        String cinPath     = hasCin     ? cloudinaryService.uploadKycFile(cinDocument, "cin")     : null;
        String selfiePath  = hasSelfie  ? cloudinaryService.uploadKycFile(selfie,      "selfie")  : null;
        String diplomaPath = hasDiploma ? cloudinaryService.uploadKycFile(diploma,     "diploma") : null;

        KycSubmitRequest request = new KycSubmitRequest();
        request.setCinNumber(cinNumber.trim());
        request.setCinDocumentPath(cinPath);
        request.setSelfiePath(selfiePath);
        request.setDiplomaDocumentPath(diplomaPath);

        return ResponseEntity.status(201)
                .body(kycRequestService.submitKycRequest(userId, request));
    }

    @PutMapping("/review/{kycRequestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KycRequestDTO> reviewKyc(
            @PathVariable Long kycRequestId,
            @RequestBody KycReviewRequest request
    ) {
        return ResponseEntity.ok(
                kycRequestService.reviewKycRequest(kycRequestId, request, "admin")
        );
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<KycRequestDTO>> getPending() {
        return ResponseEntity.ok(kycRequestService.getPendingRequests());
    }

    @GetMapping("/history/{userId}")
    @PreAuthorize("hasRole('ADMIN') or isAuthenticated()")
    public ResponseEntity<List<KycRequestDTO>> getHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(kycRequestService.getHistoryByUser(userId));
    }

}