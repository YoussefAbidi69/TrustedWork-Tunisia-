package tn.esprit.userservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ==================== RESET PASSWORD EMAIL ====================

    @Override
    public void sendResetPasswordEmail(String to, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "TrustedWork Tunisia");
            helper.setTo(to);
            helper.setSubject("TrustedWork - Réinitialisation de mot de passe");

            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 30px;">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <div style="display: inline-block; width: 50px; height: 50px; border-radius: 12px;
                                    background: linear-gradient(135deg, #22d3ee, #0891b2);
                                    line-height: 50px; color: #0f172a; font-weight: 800; font-size: 18px;">
                            TW
                        </div>
                        <h2 style="color: #111827; margin-top: 16px;">TrustedWork Tunisia</h2>
                    </div>

                    <p style="color: #374151; font-size: 15px; line-height: 1.6;">
                        Bonjour,
                    </p>
                    <p style="color: #374151; font-size: 15px; line-height: 1.6;">
                        Vous avez demandé la réinitialisation de votre mot de passe.
                        Cliquez sur le bouton ci-dessous pour choisir un nouveau mot de passe :
                    </p>

                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s"
                           style="display: inline-block; padding: 14px 32px; border-radius: 12px;
                                  background: linear-gradient(135deg, #22d3ee, #0891b2);
                                  color: #0f172a; font-weight: 700; font-size: 15px;
                                  text-decoration: none;">
                            Réinitialiser mon mot de passe
                        </a>
                    </div>

                    <p style="color: #9ca3af; font-size: 13px; line-height: 1.5;">
                        Ce lien expire dans <strong>30 minutes</strong>.<br>
                        Si vous n'avez pas fait cette demande, ignorez cet email.
                    </p>

                    <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 30px 0;">

                    <p style="color: #9ca3af; font-size: 12px; text-align: center;">
                        &copy; 2025 TrustedWork Tunisia. Tous droits réservés.
                    </p>
                </div>
                """.formatted(resetLink);

            helper.setText(html, true);

            mailSender.send(message);
            log.info("Reset password email sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send reset password email to {}: {}", to, e.getMessage());
        }
    }

    // ==================== SIMPLE EMAIL ====================

    @Override
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}