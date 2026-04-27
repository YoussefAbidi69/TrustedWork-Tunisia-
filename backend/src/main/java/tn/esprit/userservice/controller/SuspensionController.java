package tn.esprit.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.SuspensionRecordDTO;
import tn.esprit.userservice.service.ISuspensionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/suspensions")
@RequiredArgsConstructor
@Tag(name = "Suspensions", description = "Gestion des suspensions avec historique complet")
@PreAuthorize("hasRole('ADMIN')")
public class SuspensionController {

    private final ISuspensionService suspensionService;

    // Suspendre un utilisateur avec motif obligatoire
    @PostMapping("/suspend/{userId}")
    @Operation(summary = "Suspendre un utilisateur avec motif")
    public ResponseEntity<SuspensionRecordDTO> suspend(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Le motif de suspension est obligatoire");
        }

        return ResponseEntity.ok(
                suspensionService.suspendUser(userId, reason, authentication.getName()));
    }

    // Lever la suspension d'un utilisateur
    @PostMapping("/lift/{userId}")
    @Operation(summary = "Lever la suspension d'un utilisateur")
    public ResponseEntity<SuspensionRecordDTO> lift(
            @PathVariable Long userId,
            Authentication authentication) {

        return ResponseEntity.ok(
                suspensionService.liftSuspension(userId, authentication.getName()));
    }

    // Historique des suspensions d'un utilisateur
    @GetMapping("/history/{userId}")
    @Operation(summary = "Historique des suspensions d'un utilisateur")
    public ResponseEntity<List<SuspensionRecordDTO>> history(@PathVariable Long userId) {
        return ResponseEntity.ok(suspensionService.getHistory(userId));
    }

    // Toutes les suspensions actives
    @GetMapping("/active")
    @Operation(summary = "Lister toutes les suspensions actives")
    public ResponseEntity<List<SuspensionRecordDTO>> allActive() {
        return ResponseEntity.ok(suspensionService.getAllActive());
    }
}