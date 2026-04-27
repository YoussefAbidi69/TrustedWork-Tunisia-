package tn.esprit.userservice.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.dto.AuthResponse;
import tn.esprit.userservice.dto.LoginRequest;
import tn.esprit.userservice.dto.RegisterRequest;
import tn.esprit.userservice.dto.VerifyTwoFactorRequest;
import tn.esprit.userservice.entity.AccountStatus;
import tn.esprit.userservice.entity.Role;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.exception.AccountSuspendedException;
import tn.esprit.userservice.exception.EmailAlreadyExistsException;
import tn.esprit.userservice.exception.InvalidCredentialsException;
import tn.esprit.userservice.exception.InvalidTwoFactorCodeException;
import tn.esprit.userservice.mapper.UserMapper;
import tn.esprit.userservice.repository.UserRepository;
import tn.esprit.userservice.security.JwtService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ITwoFactorService twoFactorService;
    private final UserMapper userMapper;
    private final ITrustLevelService trustLevelService;

    // Nombre maximum de tentatives avant verrouillage
    private static final int MAX_FAILED_ATTEMPTS = 5;

    // Durée du verrouillage en minutes
    private static final int LOCK_DURATION_MINUTES = 15;

    // ==================== REGISTER ====================

    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        if (userRepository.existsByCin(request.getCin())) {
            throw new IllegalArgumentException("CIN already registered: " + request.getCin());
        }

        User user = new User();
        user.setCin(request.getCin());
        user.setPhoto(request.getPhoto());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhoneNumber());

        Role role = Role.valueOf(request.getRole().toUpperCase());
        user.setRole(role);

        userMapper.initNewUser(user);

        User savedUser = userRepository.save(user);

        // Calcul initial du Trust Level après création
        trustLevelService.computeAndSave(savedUser);

        log.info("Nouvel utilisateur enregistré : {} avec le rôle {}", savedUser.getEmail(), role);

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .role(role.name())
                .twoFactorRequired(false)
                .message("Registration successful")
                .build();
    }

    // ==================== LOGIN ====================

    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Vérification du verrouillage temporaire
        if (isCurrentlyLocked(user)) {
            long minutesLeft = java.time.temporal.ChronoUnit.MINUTES.between(
                    LocalDateTime.now(), user.getLockedUntil());
            throw new AccountSuspendedException(
                    "Compte temporairement verrouillé. Réessayez dans " + minutesLeft + " minute(s).");
        }

        // Vérification du mot de passe
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedAttempt(user);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Vérification du statut du compte
        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new AccountSuspendedException("Your account has been suspended");
        }

        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw new AccountSuspendedException("Your account is disabled or locked");
        }

        if (user.getRole() == null) {
            log.error("Login bloqué : l'utilisateur {} n'a pas de rôle", user.getEmail());
            throw new InvalidCredentialsException("User has no assigned role");
        }

        // Réinitialisation du compteur après connexion réussie
        resetFailedAttempts(user);

        // Mise à jour des données de session
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(request.getIpAddress());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Si 2FA activée, on ne génère pas encore les tokens
        if (user.isTwoFactorEnabled()) {
            log.info("2FA requise pour l'utilisateur {}", user.getEmail());
            return AuthResponse.builder()
                    .accessToken(null)
                    .refreshToken(null)
                    .tokenType("Bearer")
                    .userId(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .twoFactorRequired(true)
                    .message("2FA code required")
                    .build();
        }

        log.info("Connexion réussie pour {} avec le rôle {}", user.getEmail(), user.getRole());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        String primaryRole = userMapper.toDTO(user).getRole();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(primaryRole)
                .twoFactorRequired(false)
                .message("Login successful")
                .build();
    }

    // ==================== VERIFY 2FA ====================

    @Override
    public AuthResponse verifyTwoFactor(VerifyTwoFactorRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!user.isTwoFactorEnabled()) {
            throw new InvalidTwoFactorCodeException("2FA is not enabled for this account");
        }

        boolean valid = twoFactorService.verifyCode(user.getSecret2FA(), request.getCode());

        if (!valid) {
            throw new InvalidTwoFactorCodeException("Invalid 2FA code");
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        String primaryRole = userMapper.toDTO(user).getRole();

        log.info("2FA vérifiée avec succès pour {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(primaryRole)
                .twoFactorRequired(false)
                .message("2FA verification successful")
                .build();
    }

    // ==================== REFRESH TOKEN ====================

    @Override
    public AuthResponse refreshToken(String refreshToken) {

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        String email = jwtService.extractEmail(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (user.getRole() == null) {
            log.error("Refresh token bloqué : {} n'a pas de rôle", user.getEmail());
            throw new InvalidCredentialsException("User has no assigned role");
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        String primaryRole = userMapper.toDTO(user).getRole();

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(primaryRole)
                .twoFactorRequired(false)
                .message("Token refreshed")
                .build();
    }

    // ==================== HELPERS PRIVÉS ====================

    /**
     * Vérifie si le compte est actuellement dans la fenêtre de verrouillage.
     */
    private boolean isCurrentlyLocked(User user) {
        return user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    /**
     * Incrémente le compteur de tentatives échouées.
     * Verrouille le compte pendant LOCK_DURATION_MINUTES si MAX_FAILED_ATTEMPTS atteint.
     */
    private void handleFailedAttempt(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            user.setAccountNonLocked(false);
            log.warn("Compte {} verrouillé après {} tentatives échouées", user.getEmail(), attempts);
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Réinitialise le compteur et déverrouille le compte après connexion réussie.
     */
    private void resetFailedAttempts(User user) {
        if (user.getFailedAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            user.setAccountNonLocked(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }
}