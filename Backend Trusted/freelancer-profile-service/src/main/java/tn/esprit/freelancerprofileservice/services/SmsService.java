package tn.esprit.freelancerprofileservice.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service SMS — envoie des notifications via Twilio.
 * Déclenché quand un rapport de profil est résolu par l'admin.
 */
@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;

    // Numéro destinataire fixe pour démo jury (numéro vérifié Twilio sandbox)
    private static final String DEMO_RECIPIENT = "+21651837560";

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info(">>> SMS SERVICE — Twilio initialisé depuis {}", twilioPhoneNumber);
    }

    public void sendSms(String toPhone, String body) {
        if (toPhone == null || toPhone.isBlank()) {
            log.warn(">>> SMS SKIP — numéro absent");
            return;
        }

        String normalizedPhone = normalizePhone(toPhone);

        try {
            Message message = Message.creator(
                    new PhoneNumber(normalizedPhone),
                    new PhoneNumber(twilioPhoneNumber),
                    body
            ).create();

            log.info(">>> SMS ENVOYÉ — SID: {} | to: {} | status: {}",
                    message.getSid(), normalizedPhone, message.getStatus());

        } catch (com.twilio.exception.ApiException e) {
            log.error(">>> SMS ERREUR TWILIO — code: {} | {}", e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error(">>> SMS ERREUR — to: {} | {}", normalizedPhone, e.getMessage());
        }
    }

    public void sendReportResolvedSms(String firstName, Long reportId, String status) {
        String body = String.format(
                "[TrustedWork Tunisia] Bonjour %s, " +
                        "votre signalement #%d a ete traite. " +
                        "Statut : %s. " +
                        "Connectez-vous pour voir les details.",
                firstName != null ? firstName : "Freelancer",
                reportId,
                status
        );
        // Utiliser le numéro fixe de démo (sandbox Twilio)
        sendSms(DEMO_RECIPIENT, body);
    }

    private String normalizePhone(String phone) {
        String cleaned = phone.replaceAll("[\\s\\-]", "");
        if (cleaned.startsWith("+"))     return cleaned;
        if (cleaned.startsWith("00216")) return "+" + cleaned.substring(2);
        if (cleaned.startsWith("216"))   return "+" + cleaned;
        if (cleaned.length() == 8)       return "+216" + cleaned;
        return cleaned;
    }
}