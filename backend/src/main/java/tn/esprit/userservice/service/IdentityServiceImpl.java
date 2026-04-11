package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.dto.PublicUserDTO;
import tn.esprit.userservice.dto.TokenValidationDTO;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.exception.UserNotFoundException;
import tn.esprit.userservice.repository.UserRepository;
import tn.esprit.userservice.security.JwtService;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityServiceImpl implements IIdentityService {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    // ==================== VALIDATE TOKEN ====================

    @Override
    public TokenValidationDTO validateToken(String authHeader) {

        // Vérification du format Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return TokenValidationDTO.builder()
                    .valid(false)
                    .error("Authorization header manquant ou format invalide")
                    .build();
        }

        String token = authHeader.substring(7);

        // Vérification de la validité du token
        if (!jwtService.isTokenValid(token)) {
            return TokenValidationDTO.builder()
                    .valid(false)
                    .error("Token expiré ou invalide")
                    .build();
        }

        try {
            String email = jwtService.extractEmail(token);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

            // Vérification que le compte est actif
            if (!user.isEnabled() || !user.isAccountNonLocked()) {
                return TokenValidationDTO.builder()
                        .valid(false)
                        .error("Compte désactivé ou verrouillé")
                        .build();
            }

            log.info("Token validé pour l'utilisateur : {}", email);

            return TokenValidationDTO.builder()
                    .valid(true)
                    .userId(user.getId())
                    .cin(user.getCin())
                    .email(user.getEmail())
                    .role(user.getRole() != null ? user.getRole().name() : null)
                    .trustLevel(user.getTrustLevel())
                    .kycStatus(user.getKycStatus() != null
                            ? user.getKycStatus().name() : null)
                    .accountStatus(user.getAccountStatus() != null
                            ? user.getAccountStatus().name() : null)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la validation du token : {}", e.getMessage());
            return TokenValidationDTO.builder()
                    .valid(false)
                    .error("Erreur de validation : " + e.getMessage())
                    .build();
        }
    }

    // ==================== PUBLIC PROFILE ====================

    @Override
    public PublicUserDTO getPublicProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur introuvable : " + userId));

        return PublicUserDTO.builder()
                .id(user.getId())
                .cin(user.getCin())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .kycStatus(user.getKycStatus() != null
                        ? user.getKycStatus().name() : null)
                .trustLevel(user.getTrustLevel())
                .accountStatus(user.getAccountStatus() != null
                        ? user.getAccountStatus().name() : null)
                .build();
    }

    // ==================== TRUST LEVEL INFO ====================

    @Override
    public TokenValidationDTO getTrustLevelInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur introuvable : " + userId));

        return TokenValidationDTO.builder()
                .valid(true)
                .userId(user.getId())
                .email(user.getEmail())
                .trustLevel(user.getTrustLevel())
                .kycStatus(user.getKycStatus() != null
                        ? user.getKycStatus().name() : null)
                .build();
    }
}