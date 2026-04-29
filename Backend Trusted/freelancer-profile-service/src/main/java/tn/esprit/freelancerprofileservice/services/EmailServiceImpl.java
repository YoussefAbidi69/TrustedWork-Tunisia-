package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final String BONJOUR = "Bonjour ";

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.test-recipient:}")
    private String testRecipient;

    // =========================================================
    // EMAIL 1 — Résolution de signalement
    // =========================================================

    @Override
    @Async
    public void sendReportStatusEmail(String to, String fullName, String status, String description) {
        try {
            String finalRecipient = resolveRecipient(to);

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(finalRecipient);
            mail.setSubject("Mise à jour de votre signalement");
            mail.setText(buildReportMessage(fullName, status, description));

            mailSender.send(mail);
            log.info(">>> MAIL SUCCESS [report] → {}", finalRecipient);

        } catch (Exception e) {
            log.error(">>> MAIL ERROR [report] — {}", e.getMessage(), e);
        }
    }

    // =========================================================
    // EMAIL 2 — Rappel profil incomplet (Scheduler Tâche 3)
    // =========================================================

    @Override
    @Async
    public void sendProfileIncompleteReminder(String to, String fullName, int completenessScore) {
        try {
            String finalRecipient = resolveRecipient(to);
            String safeName = safeName(fullName);

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(finalRecipient);
            mail.setSubject("[TrustedWork] Complétez votre profil pour attirer plus de clients");
            mail.setText(
                    BONJOUR + safeName + ",\n\n" +
                            "Votre profil TrustedWork Tunisia est complété à " + completenessScore + "%.\n\n" +
                            "Les profils complétés à plus de 80% reçoivent 3x plus de demandes de collaboration.\n\n" +
                            "Conseils pour améliorer votre profil :\n" +
                            "  • Ajoutez une photo professionnelle\n" +
                            "  • Décrivez vos expériences professionnelles\n" +
                            "  • Ajoutez des projets dans votre portfolio\n" +
                            "  • Obtenez des endorsements sur vos compétences\n" +
                            "  • Soumettez vos certifications\n\n" +
                            "Connectez-vous sur TrustedWork Tunisia pour compléter votre profil dès maintenant.\n\n" +
                            "Cordialement,\n" +
                            "L'équipe TrustedWork Tunisia"
            );

            mailSender.send(mail);
            log.info(">>> MAIL SUCCESS [rappel profil] → {}", finalRecipient);

        } catch (Exception e) {
            log.error(">>> MAIL ERROR [rappel profil] — {}", e.getMessage(), e);
        }
    }

    // =========================================================
    // EMAIL 3 — Alerte expiration certification (Scheduler Tâche 4)
    // =========================================================

    @Override
    @Async
    public void sendCertificationExpiryAlert(String to, String fullName,
                                             String certTitle, String expiryDate) {
        try {
            String finalRecipient = resolveRecipient(to);
            String safeName = safeName(fullName);

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(finalRecipient);
            mail.setSubject("[TrustedWork] Votre certification expire bientôt — " + certTitle);
            mail.setText(
                    BONJOUR + safeName + ",\n\n" +
                            "Votre certification \"" + certTitle + "\" expire le " + expiryDate + ".\n\n" +
                            "Une certification expirée réduit votre score d'authenticité et la visibilité " +
                            "de votre profil auprès des clients.\n\n" +
                            "Pensez à la renouveler ou à la remplacer avant cette date.\n\n" +
                            "Connectez-vous sur TrustedWork Tunisia pour mettre à jour vos certifications.\n\n" +
                            "Cordialement,\n" +
                            "L'équipe TrustedWork Tunisia"
            );

            mailSender.send(mail);
            log.info(">>> MAIL SUCCESS [cert expiry] → {}", finalRecipient);

        } catch (Exception e) {
            log.error(">>> MAIL ERROR [cert expiry] — {}", e.getMessage(), e);
        }
    }

    // =========================================================
    // EMAIL 4 — Suspension automatique par IA
    // =========================================================

    @Override
    @Async
    public void sendAutoSuspensionEmail(String userId, String fullName, int negativeCount) {
        try {
            String finalRecipient = resolveRecipient("freelancer-" + userId + "@trustedwork.tn");
            String safeName = safeName(fullName);

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(finalRecipient);
            mail.setSubject("[TrustedWork Tunisia] Votre profil a été suspendu automatiquement");
            mail.setText(
                    BONJOUR + safeName + ",\n\n" +
                    "Notre système de détection automatique basé sur l'intelligence artificielle a détecté " +
                    negativeCount + " avis négatifs sur votre profil.\n\n" +
                    "Suite à cette détection, votre profil a été suspendu temporairement " +
                    "afin de protéger la communauté TrustedWork Tunisia.\n\n" +
                    "Pour contester cette décision ou demander une révision, " +
                    "veuillez contacter notre équipe de modération.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe TrustedWork Tunisia"
            );

            mailSender.send(mail);
            log.info(">>> MAIL SUCCESS [auto-suspension] → userId={}", userId);

        } catch (Exception e) {
            log.error(">>> MAIL ERROR [auto-suspension] — {}", e.getMessage(), e);
        }
    }

    // =========================================================
    // HELPERS PRIVÉS
    // =========================================================

    /**
     * En mode test : redirige tous les emails vers le testRecipient.
     * En production : utilise l'adresse réelle du destinataire.
     */
    private String resolveRecipient(String to) {
        return (testRecipient != null && !testRecipient.isBlank())
                ? testRecipient
                : to;
    }

    private String safeName(String fullName) {
        return (fullName != null && !fullName.isBlank()) ? fullName : "Freelancer";
    }

    private String buildReportMessage(String fullName, String status, String description) {
        String safeName = safeName(fullName);

        String statusMessage;
        switch (status) {
            case "IN_REVIEW":
                statusMessage = "Votre signalement est en cours d'examen.";
                break;
            case "RESOLVED":
                statusMessage = "Votre signalement a été traité par notre équipe.";
                break;
            case "REJECTED":
                statusMessage = "Après vérification, votre signalement n'a pas été retenu.";
                break;
            default:
                statusMessage = "Le statut de votre signalement a été mis à jour.";
        }

        return BONJOUR + safeName + ",\n\n"
                + statusMessage + "\n\n"
                + "Description du signalement :\n"
                + (description != null ? description : "-") + "\n\n"
                + "Cordialement,\n"
                + "L'équipe TrustedWork Tunisia";
    }
}