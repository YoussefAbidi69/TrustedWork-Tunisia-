package tn.esprit.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.userservice.dto.GoogleOAuthRequest;
import tn.esprit.userservice.service.IGoogleOAuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth APIs: Register, Login, 2FA, Refresh Token, Google OAuth2")
public class GoogleOAuthController {

    private final IGoogleOAuthService googleOAuthService;

    @PostMapping("/google")
    @Operation(summary = "Sign in with Google (OAuth2 ID Token verification)")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleOAuthRequest request) {
        return ResponseEntity.ok(googleOAuthService.loginWithGoogle(request));
    }
}
