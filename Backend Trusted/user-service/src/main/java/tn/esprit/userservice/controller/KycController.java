package tn.esprit.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.KycReviewRequest;
import tn.esprit.userservice.dto.KycSubmitRequest;
import tn.esprit.userservice.dto.UserDTO;
import tn.esprit.userservice.service.IKycService;

import java.util.List;

@RestController
@RequestMapping("/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC Verification", description = "Legacy KYC workflow")
public class KycController {

    private final IKycService kycService;

    @PostMapping("/submit/{cin}")
    @Operation(summary = "Legacy submit KYC documents for verification")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> submitKyc(@PathVariable Integer cin,
                                             @Valid @RequestBody KycSubmitRequest request) {
        return ResponseEntity.ok(kycService.submitKyc(cin, request));
    }

    @GetMapping("/status/{cin}")
    @Operation(summary = "Get KYC status for a user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getKycStatus(@PathVariable Integer cin) {
        return ResponseEntity.ok(kycService.getKycStatus(cin));
    }

    @GetMapping("/review/pending")
    @Operation(summary = "[ADMIN] List all users with pending KYC")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getPendingKyc() {
        return ResponseEntity.ok(kycService.getPendingKycRequests());
    }

    @PutMapping("/review/{cin}")
    @Operation(summary = "[ADMIN] Approve or reject KYC for a user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> reviewKyc(@PathVariable Integer cin,
                                             @Valid @RequestBody KycReviewRequest request,
                                             Authentication authentication) {
        String adminEmail = authentication.getName();
        return ResponseEntity.ok(kycService.reviewKyc(cin, request, adminEmail));
    }
}