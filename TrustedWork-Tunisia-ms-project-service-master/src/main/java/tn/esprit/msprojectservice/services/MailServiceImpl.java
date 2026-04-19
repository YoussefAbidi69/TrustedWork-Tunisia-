package tn.esprit.msprojectservice.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import tn.esprit.msprojectservice.dto.ProgressReportDTO;
import tn.esprit.msprojectservice.entities.Deliverable;
import tn.esprit.msprojectservice.entities.DeliverableStatus;
import tn.esprit.msprojectservice.entities.Project;
import tn.esprit.msprojectservice.feign.UserServiceClient;
import tn.esprit.msprojectservice.feign.dto.UserDTO;

import java.time.format.DateTimeFormatter;

/**
 * Implementation du service de mailing -- Module 08 TrustedWork Tunisia
 *
 * Phase 2 : utilise UserServiceClient (appel REST vers Module 01)
 * pour recuperer l'email reel du client et du freelancer via leur userId.
 *
 * Deux fonctionnalites :
 *   1. Rapport hebdomadaire  -> template "rapport-hebdomadaire.html"
 *   2. Review de livrable    -> template "review-livrable.html"
 */
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements IMailService {

    private static final Logger logger = LoggerFactory.getLogger(MailServiceImpl.class);

    private static final DateTimeFormatter DATE_FORMATTER     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy a HH:mm");

    private final JavaMailSender    mailSender;
    private final TemplateEngine    templateEngine;

    // Phase 2 -- Feign client vers Module 01 pour recuperer l'email reel
    private final UserServiceClient userServiceClient;

    // ============================================================
    // 1. RAPPORT HEBDOMADAIRE
    // ============================================================

    /**
     * Envoie le rapport hebdomadaire au client ET au freelancer.
     * Appele depuis DeliveryRiskScheduler chaque lundi a 8h.
     */
    @Override
    public void envoyerRapportHebdomadaire(Project project, ProgressReportDTO report) {
        logger.info("Envoi du rapport hebdomadaire -- Projet: {}", project.getTitle());

        Context context = new Context();
        context.setVariable("projetTitre",       project.getTitle());
        context.setVariable("projetDescription", project.getDescription());
        context.setVariable("completionRate",    report.getCompletionRate());
        context.setVariable("completedTasks",    report.getCompletedTasks());
        context.setVariable("totalTasks",        report.getTotalTasks());
        context.setVariable("openDeliverables",  report.getOpenDeliverables());
        context.setVariable("activeRisks",       report.getActiveRisks());
        context.setVariable("summary",           report.getSummary());
        context.setVariable("dateGeneration",
                report.getGeneratedAt() != null
                        ? report.getGeneratedAt().format(DATETIME_FORMATTER) : "N/A");
        context.setVariable("dateDebut",
                project.getStartDate() != null
                        ? project.getStartDate().format(DATE_FORMATTER) : "N/A");
        context.setVariable("dateFin",
                project.getEndDate() != null
                        ? project.getEndDate().format(DATE_FORMATTER) : "N/A");

        String contenuHtml = templateEngine.process("rapport-hebdomadaire", context);

        String sujet = String.format("[TrustedWork] Rapport hebdomadaire -- %s -- %d%% complete",
                project.getTitle(), report.getCompletionRate());

        // Phase 2 -- Recuperation des emails reels depuis Module 01
        String emailClient     = recupererEmail(project.getClientId(),     "client");
        String emailFreelancer = recupererEmail(project.getFreelancerId(), "freelancer");

        if (emailClient != null) {
            envoyerEmailHtml(emailClient, sujet, contenuHtml, "client", project.getTitle());
        }
        if (emailFreelancer != null) {
            envoyerEmailHtml(emailFreelancer, sujet, contenuHtml, "freelancer", project.getTitle());
        }
    }

    // ============================================================
    // 2. REVIEW LIVRABLE
    // ============================================================

    /**
     * Notifie le freelancer que son livrable a ete approuve ou rejete.
     * Appele depuis DeliverableServiceImpl.reviewDeliverable().
     */
    @Override
    public void envoyerNotificationReviewLivrable(Deliverable deliverable) {
        logger.info("Envoi notification review livrable -- ID: {} | Statut: {}",
                deliverable.getId(), deliverable.getStatus());

        if (deliverable.getProject() == null || deliverable.getProject().getFreelancerId() == null) {
            logger.warn("Impossible d'envoyer l'email -- projet ou freelancerId null sur le livrable {}",
                    deliverable.getId());
            return;
        }

        Long freelancerId = deliverable.getProject().getFreelancerId();

        // Phase 2 -- Recuperation de l'email reel du freelancer depuis Module 01
        String emailFreelancer = recupererEmail(freelancerId, "freelancer");
        if (emailFreelancer == null) {
            logger.warn("Email introuvable pour le freelancerId {} -- email non envoye", freelancerId);
            return;
        }

        boolean estApprouve    = deliverable.getStatus() == DeliverableStatus.APPROVED;
        String prenomFreelancer = recupererPrenom(freelancerId);

        Context context = new Context();
        context.setVariable("prenomFreelancer", prenomFreelancer);
        context.setVariable("livrableTitre",    deliverable.getTitle());
        context.setVariable("estApprouve",      estApprouve);
        context.setVariable("statut",           estApprouve ? "APPROUVE" : "REJETE");
        context.setVariable("reviewComment",    deliverable.getReviewComment());
        context.setVariable("dateReview",
                deliverable.getReviewedAt() != null
                        ? deliverable.getReviewedAt().format(DATETIME_FORMATTER) : "N/A");
        context.setVariable("projetTitre",
                deliverable.getProject().getTitle() != null
                        ? deliverable.getProject().getTitle() : "N/A");
        context.setVariable("fileUrl", deliverable.getFileUrl());

        String contenuHtml = templateEngine.process("review-livrable", context);

        String sujet = estApprouve
                ? String.format("[TrustedWork] Livrable approuve -- %s", deliverable.getTitle())
                : String.format("[TrustedWork] Livrable rejete -- %s", deliverable.getTitle());

        envoyerEmailHtml(emailFreelancer, sujet, contenuHtml, "freelancer", deliverable.getTitle());
    }

    // ============================================================
    // METHODE UTILITAIRE -- Envoi generique HTML
    // ============================================================

    private void envoyerEmailHtml(String destinataire, String sujet,
                                   String contenuHtml, String role, String contexte) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(contenuHtml, true);
            helper.setFrom("noreply@trustedwork.tn");

            mailSender.send(message);
            logger.info("Email envoye au {} ({}) -- Contexte: {}", role, destinataire, contexte);

        } catch (MessagingException e) {
            logger.error("Echec envoi email au {} ({}) -- Erreur: {}",
                    role, destinataire, e.getMessage());
        }
    }

    // ============================================================
    // METHODE UTILITAIRE -- Email reel via Module 01
    // ============================================================

    /**
     * Appelle UserServiceClient.getUserById() -> GET /api/identity/users/{userId}
     * Retourne l'email de l'utilisateur, ou null si Module 01 KO.
     */
    private String recupererEmail(Long userId, String role) {
        if (userId == null) {
            logger.warn("recupererEmail() -- userId null pour le role '{}'", role);
            return null;
        }

        UserDTO user = userServiceClient.getUserById(userId);

        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            logger.warn("Email introuvable via Module 01 pour userId={} ({})", userId, role);
            return null;
        }

        logger.debug("Email recupere pour userId={} ({}) : {}", userId, role, user.getEmail());
        return user.getEmail();
    }

    /**
     * Recupere le prenom de l'utilisateur pour personnaliser l'email.
     * Retourne "Freelancer" par defaut si Module 01 est KO.
     */
    private String recupererPrenom(Long userId) {
        if (userId == null) return "Freelancer";

        UserDTO user = userServiceClient.getUserById(userId);

        if (user == null || user.getFirstName() == null || user.getFirstName().isBlank()) {
            return "Freelancer";
        }

        return user.getFirstName();
    }
}
