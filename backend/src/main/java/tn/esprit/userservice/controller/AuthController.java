package tn.esprit.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.AuthResponse;
import tn.esprit.userservice.dto.LoginRequest;
import tn.esprit.userservice.dto.RegisterRequest;
import tn.esprit.userservice.dto.VerifyTwoFactorRequest;
import tn.esprit.userservice.service.IAuthService;
import tn.esprit.userservice.service.ITwoFactorService;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth APIs: Register, Login, 2FA, Refresh Token")
public class AuthController {

    private final IAuthService authService;
    private final ITwoFactorService twoFactorService;

    // ==================== REGISTER ====================

    @PostMapping("/register")
    @Operation(summary = "Inscription d'un nouvel utilisateur")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    // ==================== LOGIN ====================

    @PostMapping("/login")
    @Operation(summary = "Connexion avec email et mot de passe")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        // Extraction automatique de l'IP si non fournie par le client
        if (request.getIpAddress() == null || request.getIpAddress().isBlank()) {
            request.setIpAddress(extractClientIp(httpRequest));
        }
        return ResponseEntity.ok(authService.login(request));
    }

    // ==================== 2FA ====================

    @PostMapping("/verify-2fa")
    @Operation(summary = "Vérification du code 2FA après login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> verifyTwoFactor(@Valid @RequestBody VerifyTwoFactorRequest request) {
        return ResponseEntity.ok(authService.verifyTwoFactor(request));
    }

    @PostMapping("/setup-2fa/{cin}")
    @Operation(summary = "Initialisation du 2FA — retourne le QR code URI")
    public ResponseEntity<?> setupTwoFactor(@PathVariable Integer cin) {
        String qrCodeUri = twoFactorService.setupTwoFactor(cin);
        return ResponseEntity.ok(Map.of(
                "qrCodeUri", qrCodeUri,
                "message", "2FA setup initialized successfully"
        ));
    }

    @PostMapping("/confirm-2fa/{cin}")
    @Operation(summary = "Confirmation et activation du 2FA avec le premier code OTP")
    public ResponseEntity<?> confirmTwoFactor(
            @PathVariable Integer cin,
            @RequestBody Map<String, String> request) {

        String code = request.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "2FA code is required"));
        }

        twoFactorService.confirmTwoFactor(cin, code);
        return ResponseEntity.ok(Map.of("message", "2FA enabled successfully"));
    }

    @PostMapping("/disable-2fa/{cin}")
    @Operation(summary = "Désactivation du 2FA pour un utilisateur")
    public ResponseEntity<?> disableTwoFactor(@PathVariable Integer cin) {
        twoFactorService.disable2FA(cin);
        return ResponseEntity.ok(Map.of("message", "2FA disabled successfully"));
    }

    // ==================== REFRESH TOKEN ====================

    @PostMapping("/refresh")
    @Operation(summary = "Renouvellement du access token via refresh token")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token is required"));
        }

        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    // ==================== HELPER ====================

    /**
     * Extrait l'IP réelle du client en tenant compte des proxies.
     */
    private String extractClientIp(HttpServletRequest httpRequest) {
        String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return httpRequest.getRemoteAddr();
    }
}