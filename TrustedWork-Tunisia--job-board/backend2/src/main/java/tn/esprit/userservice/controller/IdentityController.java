package tn.esprit.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.PublicUserDTO;
import tn.esprit.userservice.dto.TokenValidationDTO;
import tn.esprit.userservice.service.IIdentityService;

@RestController
@RequestMapping("/identity")
@RequiredArgsConstructor
@Tag(name = "Identity Provider",
        description = "Endpoints consommés par les autres microservices pour valider les tokens et récupérer les profils utilisateurs")
public class IdentityController {

    private final IIdentityService identityService;

    /**
     * Valide un JWT et retourne les données utilisateur extraites.
     * Consommé par TOUS les microservices avant chaque opération sécurisée.
     * Pas de @PreAuthorize — ce endpoint est appelé AVANT l'authentification.
     */
    @PostMapping("/validate-token")
    @Operation(summary = "[Inter-modules] Valider un JWT et extraire les données utilisateur")
    public ResponseEntity<TokenValidationDTO> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(identityService.validateToken(authHeader));
    }

    /**
     * Retourne le profil public d'un utilisateur par son ID.
     * Consommé par Module 02, 03, 05, 06, 07, 08.
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "[Inter-modules] Profil public d'un utilisateur")
    public ResponseEntity<PublicUserDTO> getPublicProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(identityService.getPublicProfile(userId));
    }

    /**
     * Retourne le Trust Level d'un utilisateur.
     * Consommé par Module 03 (Match Score) et Module 05 (Contract Health).
     */
    @GetMapping("/users/{userId}/trust-level")
    @Operation(summary = "[Inter-modules] Trust Level d'un utilisateur")
    public ResponseEntity<TokenValidationDTO> getTrustLevel(@PathVariable Long userId) {
        return ResponseEntity.ok(identityService.getTrustLevelInfo(userId));
    }
}