package tn.esprit.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.ChangePasswordRequest;
import tn.esprit.userservice.dto.ForgotPasswordRequest;
import tn.esprit.userservice.dto.ResetPasswordRequest;
import tn.esprit.userservice.service.IPasswordService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Password Management", description = "Forgot Password / Reset Password / Change Password")
public class PasswordController {

    private final IPasswordService passwordService;

    // Endpoint public : l'utilisateur n'est pas encore connecté quand il demande un reset
    @PostMapping("/forgot-password")
    @Operation(summary = "Envoyer une demande de réinitialisation de mot de passe")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            @RequestHeader(value = "Origin", required = false) String origin) {

        String frontendUrl = "http://localhost:4200";
        if (origin != null && origin.contains("4201")) {
            frontendUrl = "http://localhost:4201";
        }

        String token = passwordService.forgotPassword(request.getEmail(), frontendUrl);

        return ResponseEntity.ok(Map.of(
                "message", "Lien de réinitialisation généré avec succès",
                "token", token
        ));
    }

    // Endpoint public : l'utilisateur clique sur le lien dans l'email (pas de token Bearer)
    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialiser le mot de passe avec token")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
    }

    // Endpoint privé : seul l'utilisateur connecté (lui-même) peut changer son mot de passe
    @PutMapping("/change-password")
    @Operation(summary = "Changer le mot de passe utilisateur connecté")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                              Authentication authentication) {
        String email = authentication.getName();
        passwordService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès"));
    }
}