package tn.esprit.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.CompleteGoogleProfileRequest;
import tn.esprit.userservice.service.IGoogleProfileService;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Profile management and user search")
public class GoogleProfileController {

    private final IGoogleProfileService googleProfileService;

    /**
     * Vérifie si le profil de l'utilisateur connecté est incomplet (après login Google).
     * GET /api/users/me/profile-complete
     */
    @GetMapping("/me/profile-complete")
    @Operation(summary = "Check if the authenticated user's Google profile is complete")
    public ResponseEntity<Map<String, Boolean>> checkProfileComplete(Authentication authentication) {
        String email = authentication.getName();
        boolean incomplete = googleProfileService.isProfileIncomplete(email);
        return ResponseEntity.ok(Map.of("incomplete", incomplete));
    }

    /**
     * Complète le profil d'un utilisateur Google (CIN, téléphone, rôle).
     * POST /api/users/me/complete-profile
     */
    @PostMapping("/me/complete-profile")
    @Operation(summary = "Complete Google user profile with CIN, phone and role")
    public ResponseEntity<?> completeProfile(
            @Valid @RequestBody CompleteGoogleProfileRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(googleProfileService.completeProfile(email, request));
    }
}
