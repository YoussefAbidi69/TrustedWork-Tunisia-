package tn.esprit.smartjobboard.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import tn.esprit.smartjobboard.entity.FreelancerProfile;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.OpportunityNotificationLog;
import tn.esprit.smartjobboard.repository.OpportunityNotificationLogRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpportunityNotificationDispatcher")
class OpportunityNotificationDispatcherTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private OpportunityNotificationLogRepository opportunityNotificationLogRepository;

    @Mock
    private MimeMessage mimeMessageMock;

    @InjectMocks
    private OpportunityNotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessageMock);
        ReflectionTestUtils.setField(dispatcher, "applyLinkBase", "http://localhost:8080/apply");
    }

    @Test
    @DisplayName("should skip sending if fromAddress is null or blank")
    void skipEmptyFromAddress() {
        ReflectionTestUtils.setField(dispatcher, "fromAddress", "");
        
        JobOffer job = new JobOffer();
        job.setId(1L);
        FreelancerProfile fp = new FreelancerProfile();
        fp.setUserId(2L);

        dispatcher.notifyFreelancer(job, fp, 85.0);

        verifyNoInteractions(mailSender);
        verifyNoInteractions(opportunityNotificationLogRepository);
    }

    @Test
    @DisplayName("should send email and log to db successfully")
    void sendSuccess() {
        ReflectionTestUtils.setField(dispatcher, "fromAddress", "noreply@trustedwork.com");

        JobOffer job = new JobOffer();
        job.setId(1L);
        job.setTitle("Java Dev");

        FreelancerProfile fp = new FreelancerProfile();
        fp.setUserId(2L);
        fp.setEmail("freelancer@test.com");

        dispatcher.notifyFreelancer(job, fp, 95.5);

        verify(mailSender).send(mimeMessageMock);
        verify(opportunityNotificationLogRepository).save(any(OpportunityNotificationLog.class));
    }

    @Test
    @DisplayName("should handle mail sender exceptions gracefully")
    void handleMailSenderException() {
        ReflectionTestUtils.setField(dispatcher, "fromAddress", "noreply@trustedwork.com");

        JobOffer job = new JobOffer();
        job.setId(1L);
        job.setTitle("Java Dev");

        FreelancerProfile fp = new FreelancerProfile();
        fp.setUserId(2L);
        fp.setEmail("freelancer@test.com");

        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(mimeMessageMock);

        dispatcher.notifyFreelancer(job, fp, 95.5);

        verify(mailSender).send(mimeMessageMock);
        // It failed, so no log is saved
        verifyNoInteractions(opportunityNotificationLogRepository);
    }
}
