package tn.esprit.freelancerprofileservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.request.AddCertificationRequest;
import tn.esprit.freelancerprofileservice.dto.response.CertificationResponse;
import tn.esprit.freelancerprofileservice.entities.Certification;
import tn.esprit.freelancerprofileservice.services.ICertificationService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST — gestion des certifications
 */
@RestController
@RequestMapping("/api/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final ICertificationService certificationService;

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

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(certificationService.addCertification(userId, cert)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CertificationResponse>> getMyCertifications(
            @PathVariable Long userId) {
        List<CertificationResponse> certs = certificationService.getMyCertifications(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(certs);
    }

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