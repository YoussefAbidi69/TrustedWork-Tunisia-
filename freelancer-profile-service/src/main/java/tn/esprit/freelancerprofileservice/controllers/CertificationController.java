package tn.esprit.freelancerprofileservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.request.AddCertificationRequest;
import tn.esprit.freelancerprofileservice.dto.request.UpdateCertificationRequest;
import tn.esprit.freelancerprofileservice.dto.response.CertificationResponse;
import tn.esprit.freelancerprofileservice.entities.Certification;
import tn.esprit.freelancerprofileservice.services.ICertificationService;

import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

/**
 * Controller REST — gestion des certifications
 */
@RestController
@RequestMapping("/api/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final ICertificationService certificationService;

    @Operation(summary = "Ajouter une certification")
    @PostMapping("/user/{userId}")
    public ResponseEntity<CertificationResponse> addCertification(
            @PathVariable Long userId,
            @Valid @RequestBody AddCertificationRequest request) {

        Certification cert = Certification.builder()
                .title(request.getTitle())
                .issuer(request.getIssuer())
                .type(request.getType())
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
                .certificateUrl(request.getCertificateUrl())
                .build();

        Certification saved = certificationService.addCertification(userId, cert);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(saved));
    }

    @Operation(summary = "Mettre à jour une certification")
    @PutMapping("/{certId}/user/{userId}")
    public ResponseEntity<CertificationResponse> updateCertification(
            @PathVariable Long certId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateCertificationRequest request) {

        Certification updated = certificationService.updateCertification(certId, userId, request);

        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(summary = "Récupérer mes certifications")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CertificationResponse>> getMyCertifications(
            @PathVariable Long userId) {

        List<CertificationResponse> certs = certificationService.getMyCertifications(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(certs);
    }

    @Operation(summary = "Supprimer une certification")
    @DeleteMapping("/{certId}/user/{userId}")
    public ResponseEntity<Void> deleteCertification(
            @PathVariable Long certId,
            @PathVariable Long userId) {

        certificationService.deleteCertification(certId, userId);

        return ResponseEntity.noContent().build();
    }

    private CertificationResponse toResponse(Certification c) {
        return CertificationResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .issuer(c.getIssuer())
                .type(c.getType())
                .issueDate(c.getIssueDate())
                .expiryDate(c.getExpiryDate())
                .certificateUrl(c.getCertificateUrl())
                .isExpired(c.getIsExpired())
                .build();
    }
}