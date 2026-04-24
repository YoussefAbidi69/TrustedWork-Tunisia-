package tn.esprit.msprojectservice.services;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tn.esprit.msprojectservice.dto.ProgressReportDTO;
import tn.esprit.msprojectservice.entities.*;
import tn.esprit.msprojectservice.feign.UserServiceClient;
import tn.esprit.msprojectservice.feign.dto.UserDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MailServiceImpl — Tests unitaires")
class MailServiceImplTest {

    @Mock private JavaMailSender       mailSender;
    @Mock private SpringTemplateEngine templateEngine;
    @Mock private UserServiceClient    userServiceClient;

    private MailServiceImpl mailService;
    private MimeMessage     mimeMessage;

    @BeforeEach
    void setUp() {
        mailService = new MailServiceImpl(mailSender, templateEngine, userServiceClient);
        mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html><body>Test Email</body></html>");
    }

    // ─── envoyerRapportHebdomadaire ───────────────────────────────────────────

    @Test
    @DisplayName("envoyerRapportHebdomadaire — envoie email au client ET au freelancer")
    void envoyerRapportHebdomadaire_bothEmails_sent() {
        Project project = buildProject(10L, 20L);
        ProgressReportDTO report = buildReport();

        when(userServiceClient.getUserById(10L)).thenReturn(
                UserDTO.builder().email("client@test.tn").build());
        when(userServiceClient.getUserById(20L)).thenReturn(
                UserDTO.builder().email("freelancer@test.tn").build());

        mailService.envoyerRapportHebdomadaire(project, report);

        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("envoyerRapportHebdomadaire — email client null → uniquement freelancer")
    void envoyerRapportHebdomadaire_clientEmailNull_onlyFreelancerEmailSent() {
        Project project = buildProject(10L, 20L);
        ProgressReportDTO report = buildReport();

        when(userServiceClient.getUserById(10L)).thenReturn(null);
        when(userServiceClient.getUserById(20L)).thenReturn(
                UserDTO.builder().email("freelancer@test.tn").build());

        mailService.envoyerRapportHebdomadaire(project, report);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("envoyerRapportHebdomadaire — email freelancer null → uniquement client")
    void envoyerRapportHebdomadaire_freelancerEmailNull_onlyClientEmailSent() {
        Project project = buildProject(10L, 20L);
        ProgressReportDTO report = buildReport();

        when(userServiceClient.getUserById(10L)).thenReturn(
                UserDTO.builder().email("client@test.tn").build());
        when(userServiceClient.getUserById(20L)).thenReturn(null);

        mailService.envoyerRapportHebdomadaire(project, report);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("envoyerRapportHebdomadaire — clientId null → pas d'email client envoyé")
    void envoyerRapportHebdomadaire_nullClientId_skipClientEmail() {
        Project project = buildProject(null, 20L);
        ProgressReportDTO report = buildReport();

        when(userServiceClient.getUserById(20L)).thenReturn(
                UserDTO.builder().email("freelancer@test.tn").build());

        mailService.envoyerRapportHebdomadaire(project, report);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("envoyerRapportHebdomadaire — aucun email disponible → aucun envoi")
    void envoyerRapportHebdomadaire_noEmails_nothingSent() {
        Project project = buildProject(10L, 20L);
        ProgressReportDTO report = buildReport();

        when(userServiceClient.getUserById(anyLong())).thenReturn(
                UserDTO.builder().email(null).build());

        mailService.envoyerRapportHebdomadaire(project, report);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("envoyerRapportHebdomadaire — dates nulles dans rapport → 'N/A' dans template")
    void envoyerRapportHebdomadaire_nullDates_doesNotThrow() {
        Project project = Project.builder().id(1L).title("Projet Sans Dates")
                .clientId(10L).freelancerId(20L)
                .startDate(null).endDate(null).build();
        ProgressReportDTO report = ProgressReportDTO.builder()
                .completionRate(50).totalTasks(10).completedTasks(5)
                .activeRisks(1).openDeliverables(2).generatedAt(null).build();

        when(userServiceClient.getUserById(10L)).thenReturn(
                UserDTO.builder().email("client@test.tn").build());
        when(userServiceClient.getUserById(20L)).thenReturn(
                UserDTO.builder().email("freelancer@test.tn").build());

        mailService.envoyerRapportHebdomadaire(project, report);

        verify(templateEngine).process(eq("rapport-hebdomadaire"), any(Context.class));
    }

    // ─── envoyerNotificationReviewLivrable ───────────────────────────────────

    @Test
    @DisplayName("envoyerNotificationReviewLivrable — livrable APPROUVÉ notifie freelancer")
    void envoyerNotificationReviewLivrable_approved_sendsEmail() {
        Deliverable deliverable = buildDeliverable(DeliverableStatus.APPROVED);

        when(userServiceClient.getUserById(20L)).thenReturn(
                UserDTO.builder().email("freelancer@test.tn").firstName("Youssef").build());

        mailService.envoyerNotificationReviewLivrable(deliverable);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(templateEngine).process(eq("review-livrable"), any(Context.class));
    }

    @Test
    @DisplayName("envoyerNotificationReviewLivrable — livrable REJETÉ notifie freelancer")
    void envoyerNotificationReviewLivrable_rejected_sendsEmail() {
        Deliverable deliverable = buildDeliverable(DeliverableStatus.REJECTED);

        when(userServiceClient.getUserById(20L)).thenReturn(
                UserDTO.builder().email("freelancer@test.tn").firstName("Ahmed").build());

        mailService.envoyerNotificationReviewLivrable(deliverable);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("envoyerNotificationReviewLivrable — projet null → aucun envoi")
    void envoyerNotificationReviewLivrable_nullProject_returnsEarly() {
        Deliverable deliverable = Deliverable.builder()
                .id(1L).title("Livrable Test").status(DeliverableStatus.APPROVED)
                .project(null).build();

        mailService.envoyerNotificationReviewLivrable(deliverable);

        verify(mailSender, never()).send(any(MimeMessage.class));
        verifyNoInteractions(userServiceClient);
    }

    @Test
    @DisplayName("envoyerNotificationReviewLivrable — freelancerId null sur projet → aucun envoi")
    void envoyerNotificationReviewLivrable_nullFreelancerId_returnsEarly() {
        Project project = Project.builder().id(1L).title("Projet").freelancerId(null).build();
        Deliverable deliverable = Deliverable.builder()
                .id(1L).title("Livrable").status(DeliverableStatus.APPROVED)
                .project(project).build();

        mailService.envoyerNotificationReviewLivrable(deliverable);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("envoyerNotificationReviewLivrable — email freelancer introuvable → aucun envoi")
    void envoyerNotificationReviewLivrable_emailNotFound_returnsEarly() {
        Deliverable deliverable = buildDeliverable(DeliverableStatus.APPROVED);

        when(userServiceClient.getUserById(20L)).thenReturn(null);

        mailService.envoyerNotificationReviewLivrable(deliverable);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("envoyerNotificationReviewLivrable — prénom null → 'Freelancer' par défaut")
    void envoyerNotificationReviewLivrable_nullFirstName_usesDefaultPrenom() {
        Deliverable deliverable = buildDeliverable(DeliverableStatus.APPROVED);

        when(userServiceClient.getUserById(20L)).thenReturn(
                UserDTO.builder().email("freelancer@test.tn").firstName(null).build());

        mailService.envoyerNotificationReviewLivrable(deliverable);

        // Should still send the email with "Freelancer" as default name
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("envoyerNotificationReviewLivrable — reviewedAt null → 'N/A' dans template")
    void envoyerNotificationReviewLivrable_nullReviewedAt_handledGracefully() {
        Deliverable deliverable = Deliverable.builder()
                .id(1L).title("Livrable Test")
                .status(DeliverableStatus.APPROVED)
                .project(buildProject(10L, 20L))
                .reviewedAt(null)
                .reviewComment("Bon travail")
                .fileUrl("https://example.com/file.pdf")
                .build();

        when(userServiceClient.getUserById(20L)).thenReturn(
                UserDTO.builder().email("freelancer@test.tn").firstName("Youssef").build());

        mailService.envoyerNotificationReviewLivrable(deliverable);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Project buildProject(Long clientId, Long freelancerId) {
        return Project.builder()
                .id(1L).title("Projet TrustedWork")
                .clientId(clientId).freelancerId(freelancerId)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(20))
                .build();
    }

    private ProgressReportDTO buildReport() {
        return ProgressReportDTO.builder()
                .id(1L).projectId(1L)
                .completionRate(60)
                .totalTasks(10).completedTasks(6)
                .activeRisks(2).openDeliverables(1)
                .summary("Bon avancement")
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private Deliverable buildDeliverable(DeliverableStatus status) {
        return Deliverable.builder()
                .id(1L)
                .title("Livrable Test")
                .description("Description du livrable")
                .status(status)
                .project(buildProject(10L, 20L))
                .reviewedAt(LocalDateTime.now())
                .reviewComment("Commentaire de review")
                .fileUrl("https://example.com/livrable.pdf")
                .build();
    }
}
