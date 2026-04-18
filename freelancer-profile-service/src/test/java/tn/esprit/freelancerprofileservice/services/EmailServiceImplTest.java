package tn.esprit.freelancerprofileservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "from", "noreply@trustedwork.tn");
        ReflectionTestUtils.setField(emailService, "testRecipient", "");
    }

    @Test
    void sendReportStatusEmail_shouldSendEmail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendReportStatusEmail(
                "user@test.com",
                "Ahmed Ben Ali",
                "RESOLVED",
                "Profil signale pour spam"
        );

        verify(mailSender, timeout(2000)).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getTo()).contains("user@test.com");
        assertThat(sent.getSubject()).contains("signalement");
        assertThat(sent.getText()).contains("Ahmed Ben Ali");
    }

    @Test
    void sendReportStatusEmail_shouldRedirectToTestRecipient_whenTestRecipientSet() {
        ReflectionTestUtils.setField(emailService, "testRecipient", "test@trustedwork.tn");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendReportStatusEmail("real@user.com", "Ahmed", "IN_REVIEW", "desc");

        verify(mailSender, timeout(2000)).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getTo()).contains("test@trustedwork.tn");
    }

    @Test
    void sendReportStatusEmail_shouldNotThrow_whenMailSenderFails() {
        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        assertThatCode(() ->
                emailService.sendReportStatusEmail("user@test.com", "Ahmed", "RESOLVED", "desc")
        ).doesNotThrowAnyException();

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendProfileIncompleteReminder_shouldSendEmailWithCompleteness() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendProfileIncompleteReminder("user@test.com", "Sami Trabelsi", 45);

        verify(mailSender, timeout(2000)).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getText()).contains("45%");
        assertThat(sent.getText()).contains("Sami Trabelsi");
        assertThat(sent.getSubject()).contains("Complétez");
    }

    @Test
    void sendProfileIncompleteReminder_shouldUseFallbackName_whenFullNameIsNull() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendProfileIncompleteReminder("user@test.com", null, 30);

        verify(mailSender, timeout(2000)).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getText()).contains("Freelancer");
    }

    @Test
    void sendCertificationExpiryAlert_shouldSendEmailWithCertTitle() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendCertificationExpiryAlert(
                "user@test.com",
                "Rania Jrad",
                "AWS Solutions Architect",
                "2026-01-01"
        );

        verify(mailSender, timeout(2000)).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getText()).contains("AWS Solutions Architect");
        assertThat(sent.getText()).contains("Rania Jrad");
        assertThat(sent.getSubject()).contains("AWS Solutions Architect");
    }

    @Test
    void sendCertificationExpiryAlert_shouldUseFallbackName_whenBlankName() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendCertificationExpiryAlert("user@test.com", "  ", "GCP", "2026-06-01");

        verify(mailSender, timeout(2000)).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getText()).contains("Freelancer");
    }
}