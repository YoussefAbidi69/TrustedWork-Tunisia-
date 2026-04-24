package tn.esprit.userservice.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.dto.AuthResponse;
import tn.esprit.userservice.dto.GoogleOAuthRequest;
import tn.esprit.userservice.entity.AccountStatus;
import tn.esprit.userservice.entity.KycStatus;
import tn.esprit.userservice.entity.Role;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.exception.AccountSuspendedException;
import tn.esprit.userservice.repository.UserRepository;
import tn.esprit.userservice.security.JwtService;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthServiceImpl implements IGoogleOAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${google.oauth2.client-id}")
    private String googleClientId;

    @Override
    public AuthResponse loginWithGoogle(GoogleOAuthRequest request) {

        // 1. Vérifier le token Google
        GoogleIdToken.Payload payload = verifyGoogleToken(request.getCredential());

        String email = payload.getEmail();
        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");
        String picture = (String) payload.get("picture");
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());

        if (!emailVerified) {
            throw new IllegalArgumentException("Google account email is not verified");
        }

        log.info("Google OAuth login attempt for email: {}", email);

        // 2. Retrouver ou créer l'utilisateur
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createGoogleUser(email, firstName, lastName, picture));

        // 3. Vérifier le statut du compte
        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new AccountSuspendedException("Your account has been suspended");
        }

        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw new AccountSuspendedException("Your account is disabled or locked");
        }

        // 4. Générer les tokens JWT applicatifs
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Google OAuth login successful for user: {} (role: {})", email, user.getRole());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .twoFactorRequired(false)
                .message("Google login successful")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vérification du ID Token Google
    // ─────────────────────────────────────────────────────────────────────────

    private GoogleIdToken.Payload verifyGoogleToken(String credential) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);

            if (idToken == null) {
                throw new IllegalArgumentException("Invalid Google ID token");
            }

            return idToken.getPayload();

        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to verify Google ID token", e);
            throw new IllegalArgumentException("Failed to verify Google token: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Création d'un utilisateur Google (premier login)
    // ─────────────────────────────────────────────────────────────────────────

    private User createGoogleUser(String email, String firstName, String lastName, String picture) {
        log.info("Creating new user from Google OAuth: {}", email);

        User user = new User();

        // CIN obligatoire → valeur temporaire unique pour les utilisateurs Google
        // L'utilisateur devra compléter son profil avec son vrai CIN
        user.setCin(generateTemporaryCin());

        user.setEmail(email);
        user.setFirstName(firstName != null ? firstName : email.split("@")[0]);
        user.setLastName(lastName != null ? lastName : "");
        user.setPhoto(picture);

        // Mot de passe aléatoire (inutilisable directement — login Google uniquement)
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        // Rôle par défaut
        user.setRole(Role.FREELANCER);

        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setKycStatus(KycStatus.PENDING);
        user.setTwoFactorEnabled(false);

        User saved = userRepository.save(user);
        log.info("New Google user created with id={}", saved.getId());
        return saved;
    }

    /**
     * Génère un CIN temporaire négatif unique (pour contourner la contrainte NOT NULL UNIQUE).
     * L'administrateur ou l'utilisateur devra le mettre à jour avec le vrai CIN.
     */
    private Integer generateTemporaryCin() {
        int candidate;
        do {
            // CIN temporaire dans la plage négative pour éviter les collisions avec les vrais CINs
            candidate = -(int) (Math.random() * 900_000_000 + 100_000_000);
        } while (userRepository.existsByCin(candidate));
        return candidate;
    }
}
