package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.userservice.dto.AuthResponse;
import tn.esprit.userservice.dto.CompleteGoogleProfileRequest;
import tn.esprit.userservice.entity.Role;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.exception.InvalidCredentialsException;
import tn.esprit.userservice.repository.UserRepository;
import tn.esprit.userservice.security.JwtService;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleProfileServiceImpl implements IGoogleProfileService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    // ─────────────────────────────────────────────────────────────────────────
    // Vérifier si le profil est incomplet
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean isProfileIncomplete(String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getCin() != null && user.getCin() < 0)
                .orElse(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Compléter le profil Google
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse completeProfile(String email, CompleteGoogleProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        // Vérifier que le profil est bien incomplet (sinon bloquer la modification)
        if (user.getCin() != null && user.getCin() > 0) {
            throw new IllegalStateException("Profile is already complete");
        }

        // Vérifier que le CIN n'est pas déjà utilisé par un autre compte
        int newCin = Integer.parseInt(request.getCin());
        if (userRepository.existsByCin(newCin)) {
            throw new IllegalArgumentException("This CIN is already registered");
        }

        // Appliquer les modifications
        user.setCin(newCin);

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            user.setPhone(request.getPhoneNumber());
        }

        if (request.getRole() != null) {
            user.setRole(Role.valueOf(request.getRole().toUpperCase()));
        }

        userRepository.save(user);

        log.info("Google profile completed for user {} — CIN={}, role={}", email, newCin, user.getRole());

        // Regénérer les tokens avec le rôle mis à jour
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .twoFactorRequired(false)
                .message("Profile completed successfully")
                .build();
    }
}
