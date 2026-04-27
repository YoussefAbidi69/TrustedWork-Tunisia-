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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/kyc/requests")
@RequiredArgsConstructor
@Tag(name = "KYC Requests", description = "Gestion avancée KYC")
public class KycRequestController {

    private final IKycRequestService kycRequestService;

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

        // Cas 1 : soumission complète (PENDING / REJECTED / IN_REVIEW)
        if (hasCin || hasSelfie) {
            if (!hasCin)    throw new IllegalArgumentException("Le document CIN est obligatoire");
            if (!hasSelfie) throw new IllegalArgumentException("Le selfie est obligatoire");
        }

        // Cas 2 : diplôme seul (APPROVED) — au moins un fichier requis
        if (!hasCin && !hasSelfie && !hasDiploma) {
            throw new IllegalArgumentException("Au moins un document est requis");
        }

        String cinPath     = hasCin     ? saveFile(cinDocument, "cin")     : null;
        String selfiePath  = hasSelfie  ? saveFile(selfie,      "selfie")  : null;
        String diplomaPath = hasDiploma ? saveFile(diploma,     "diploma") : null;

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

    private String saveFile(MultipartFile file, String prefix) {
        try {
            String originalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().replaceAll("[\\\\/:*?\"<>|]", "_")
                    : "file";

            String fileName = prefix + "_" + UUID.randomUUID() + "_" + originalName;

            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "kyc");
            Files.createDirectories(uploadDir);

            Path destination = uploadDir.resolve(fileName);
            file.transferTo(destination.toFile());

            return "/uploads/kyc/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Erreur upload fichier KYC", e);
        }
    }
}