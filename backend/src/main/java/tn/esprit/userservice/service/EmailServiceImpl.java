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

    // ==================== AUTO ASSIGN TASK EMAIL ====================

    @Override
    public void sendAutoAssignTaskEmail(String to, String memberFirstName, String agencyName, String taskName, String projectName, String priority, String deadline, String description) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("trustedworktunisia@gmail.com", "TrustedWork Tunisia");
            helper.setTo(to);
            helper.setSubject("Nouvelle tâche assignée : " + taskName);

            String safeDescription = description != null ? description : "Aucune description";
            String safeDeadline = deadline != null ? deadline : "Aucune date limite";

            String badgeColor = "#059669"; // Green text
            String badgeBg = "#d1fae5";    // Green bg
            if ("HIGH".equalsIgnoreCase(priority) || "HAUTE".equalsIgnoreCase(priority) || "URGENT".equalsIgnoreCase(priority)) {
                badgeColor = "#dc2626"; // Red text
                badgeBg = "#fee2e2";    // Red bg
            } else if ("MEDIUM".equalsIgnoreCase(priority) || "MOYENNE".equalsIgnoreCase(priority)) {
                badgeColor = "#d97706"; // Orange text
                badgeBg = "#fef3c7";    // Orange bg
            }

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin: 0; padding: 0; background-color: #f4f4f5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f5; padding: 40px 0;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06); border: 1px solid #e4e4e7;">
                                <!-- Header -->
                                <tr>
                                    <td style="background-color: #0ea5e9; padding: 32px 40px; text-align: center;">
                                        <table width="100%%" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td align="center" style="padding-bottom: 16px;">
                                                    <div style="display: inline-block; width: 48px; height: 48px; border-radius: 12px; background-color: #ffffff; line-height: 48px; color: #0ea5e9; font-weight: 800; font-size: 18px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                                        TW
                                                    </div>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td align="center">
                                                    <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 600; letter-spacing: -0.5px;">Nouvelle tâche assignée</h1>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Body -->
                                <tr>
                                    <td style="padding: 40px;">
                                        <p style="color: #18181b; font-size: 16px; margin: 0 0 24px 0;">Bonjour <strong>%s</strong>,</p>
                                        
                                        <p style="color: #52525b; font-size: 15px; line-height: 1.6; margin: 0 0 32px 0;">
                                            Une nouvelle tâche vous a été assignée dans le cadre de l'agence <strong>%s</strong>. Voici les détails de votre mission :
                                        </p>

                                        <!-- Task Details Card -->
                                        <div style="background-color: #fafafa; border: 1px solid #e4e4e7; border-radius: 8px; padding: 24px;">
                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td colspan="2" style="padding: 0 0 20px 0; border-bottom: 1px solid #e4e4e7;">
                                                        <h2 style="color: #18181b; font-size: 18px; margin: 0; font-weight: 600;">
                                                            🎯 %s
                                                        </h2>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td style="padding: 20px 0 16px 0; width: 30%%; color: #71717a; font-size: 14px;">📁 Projet</td>
                                                    <td style="padding: 20px 0 16px 0; color: #18181b; font-size: 14px; font-weight: 500;">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style="padding: 0 0 16px 0; color: #71717a; font-size: 14px;">⚡ Priorité</td>
                                                    <td style="padding: 0 0 16px 0;">
                                                        <span style="background-color: %s; color: %s; padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; letter-spacing: 0.5px;">%s</span>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td style="padding: 0 0 16px 0; color: #71717a; font-size: 14px;">📅 Échéance</td>
                                                    <td style="padding: 0 0 16px 0; color: #18181b; font-size: 14px; font-weight: 500;">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style="padding: 0 0 0 0; color: #71717a; font-size: 14px; vertical-align: top;">📝 Description</td>
                                                    <td style="padding: 0 0 0 0; color: #3f3f46; font-size: 14px; line-height: 1.5;">%s</td>
                                                </tr>
                                            </table>
                                        </div>

                                        <!-- CTA -->
                                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin-top: 32px;">
                                            <tr>
                                                <td align="center">
                                                    <a href="http://localhost:4200/app/agencies" style="display: inline-block; background-color: #0ea5e9; color: #ffffff; text-decoration: none; font-size: 15px; font-weight: 600; padding: 14px 28px; border-radius: 6px;">
                                                        Voir ma tâche &rarr;
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #18181b; padding: 32px 40px; text-align: center;">
                                        <p style="color: #a1a1aa; font-size: 14px; margin: 0 0 8px 0;">&copy; 2026 TrustedWork Tunisia</p>
                                        <p style="color: #a1a1aa; font-size: 14px; margin: 0 0 24px 0;"><a href="mailto:trustedworktunisia@gmail.com" style="color: #38bdf8; text-decoration: none;">trustedworktunisia@gmail.com</a></p>
                                        <p style="color: #71717a; font-size: 12px; margin: 0; line-height: 1.5;">Cet email a été généré automatiquement, merci de ne pas y répondre.</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
                </body>
                </html>
                """.formatted(memberFirstName, agencyName, taskName, projectName, badgeBg, badgeColor, priority, safeDeadline, safeDescription);

            helper.setText(html, true);

            mailSender.send(message);
            log.info("Auto assign task email sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send auto assign task email to {}: {}", to, e.getMessage());
            e.printStackTrace();
        }
    }
}