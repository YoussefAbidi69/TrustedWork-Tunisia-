package tn.esprit.mscontractservicee.service.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
public class JavaMailAppEmailService implements AppEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:}")
    private String from;

    @Override
    public void sendSignatureRequestEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            if (from != null && !from.isBlank()) {
                msg.setFrom(from);
            }
            msg.setTo(toEmail);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Signature request email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send signature request email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendSimpleEmail(String toEmail, String subject, String body) {
        log.info("Preparing to send simple email to {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (from != null && !from.isBlank()) {
                message.setFrom(from);
            }
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Simple email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send simple email to {}", toEmail, e);
        }
    }
}
