package tn.esprit.mscontractservicee.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopAppEmailService implements AppEmailService {
    @Override
    public void sendSignatureRequestEmail(String toEmail, String subject, String body) {
        // Dev-friendly behavior when no SMTP is configured.
        log.warn("Mail disabled (app.mail.enabled=false). Would have sent email to={} subject={}\n{}",
                toEmail, subject, body);
    }
}

