package tn.esprit.userservice.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.exception.InvalidTwoFactorCodeException;
import tn.esprit.userservice.exception.UserNotFoundException;
import tn.esprit.userservice.repository.UserRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorServiceImpl implements ITwoFactorService {

    private final UserRepository userRepository;

    @Value("${app.2fa.issuer:TrustedWorkTunisia}")
    private String issuer;

    @Override
    public String generateSecret() {
        SecretGenerator secretGenerator = new DefaultSecretGenerator(32);
        return secretGenerator.generate();
    }

    @Override
    public String getQrCodeUri(String secret, String email) {
        if (secret == null || secret.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("Secret and email must not be null or empty");
        }

        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                issuer,
                email,
                secret,
                issuer
        );
    }

    @Override
    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || code.isBlank()) {
            return false;
        }

        String normalizedCode = code.trim();

        CodeVerifier verifier = new DefaultCodeVerifier(
                new DefaultCodeGenerator(HashingAlgorithm.SHA1),
                new SystemTimeProvider()
        );

        return verifier.isValidCode(secret, normalizedCode);
    }

    @Override
    public String setupTwoFactor(Integer cin) {
        User user = userRepository.findByCin(cin)
                .orElseThrow(() -> new UserNotFoundException("User not found with cin: " + cin));

        String secret = user.getSecret2FA();

        if (secret == null || secret.isBlank()) {
            secret = generateSecret();
            user.setSecret2FA(secret);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        String qrCodeUri = getQrCodeUri(secret, user.getEmail());

        log.info("2FA setup initialized for user: {}", user.getEmail());

        return qrCodeUri;
    }

    @Override
    public void confirmTwoFactor(Integer cin, String code) {
        User user = userRepository.findByCin(cin)
                .orElseThrow(() -> new UserNotFoundException("User not found with cin: " + cin));

        String secret = user.getSecret2FA();

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("2FA setup not initialized for this user");
        }

        boolean valid = verifyCode(secret, code);

        if (!valid) {
            throw new InvalidTwoFactorCodeException("Invalid 2FA code");
        }

        user.setTwoFactorEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("2FA confirmed and enabled for user: {}", user.getEmail());
    }

    @Override
    public void disable2FA(Integer cin) {
        User user = userRepository.findByCin(cin)
                .orElseThrow(() -> new UserNotFoundException("User not found with cin: " + cin));

        user.setSecret2FA(null);
        user.setTwoFactorEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("2FA disabled for user: {}", user.getEmail());
    }
}