package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.entity.PasswordResetToken;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.repository.PasswordResetTokenRepository;
import tn.esprit.userservice.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordServiceImpl implements IPasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;

    // ==================== FORGOT PASSWORD ====================
    @Override
    public String forgotPassword(String email, String frontendUrl) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Aucun utilisateur trouvé avec cet email"));

        // Génération token
        String token = UUID.randomUUID().toString();

        // Création token avec expiration 30 min
        PasswordResetToken resetToken =
                new PasswordResetToken(token, user.getEmail(), 30);

        tokenRepository.save(resetToken);

        // Lien frontend dynamique (frontoffice ou backoffice)
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        // Envoi de l'email
        emailService.sendResetPasswordEmail(user.getEmail(), resetLink);

        log.info("RESET PASSWORD LINK for {} : {}", email, resetLink);

        return token; // retourné aussi pour test Postman / démo
    }

    // ==================== RESET PASSWORD ====================
    @Override
    public void resetPassword(String token, String newPassword) {

        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new RuntimeException("Token invalide ou déjà utilisé"));

        // 🔥 Vérifier expiration
        if (resetToken.isExpired()) {
            throw new RuntimeException("Token expiré");
        }

        // 🔥 Récupérer user
        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // 🔥 Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 🔥 Marquer token utilisé
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password reset successful for {}", user.getEmail());
    }

    // ==================== CHANGE PASSWORD ====================
    @Override
    public void changePassword(String email, String currentPassword, String newPassword) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully for {}", email);
    }
}