package tn.esprit.smartjobboard.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tn.esprit.smartjobboard.entity.FreelancerProfile;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.OpportunityNotificationLog;
import tn.esprit.smartjobboard.repository.OpportunityNotificationLogRepository;

/**
 * Sends opportunity-agent emails asynchronously and persists one audit row per successful delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpportunityNotificationDispatcher {

    private final JavaMailSender mailSender;
    private final OpportunityNotificationLogRepository opportunityNotificationLogRepository;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${jobboard.opportunity.apply-link-base}")
    private String applyLinkBase;

    @Async
    public void notifyFreelancer(JobOffer job, FreelancerProfile freelancer, double matchScore) {
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("spring.mail.username is not set; skip opportunity email job={} freelancer={}",
                    job.getId(), freelancer.getUserId());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(freelancer.getEmail());
            helper.setSubject("TrustedWork — Job match: " + job.getTitle());
            String link = applyLinkBase + "?jobId=" + job.getId();
            String body = "A new published job closely matches your profile.\n\n"
                    + "Job: " + job.getTitle() + "\n"
                    + "Match score: " + String.format("%.2f", matchScore) + "\n"
                    + "Apply / view: " + link + "\n";
            helper.setText(body, false);
            mailSender.send(message);

            OpportunityNotificationLog row = new OpportunityNotificationLog();
            row.setJobOfferId(job.getId());
            row.setFreelancerId(freelancer.getUserId());
            row.setMatchScore(matchScore);
            row.setRecipientEmail(freelancer.getEmail());
            opportunityNotificationLogRepository.save(row);
            log.info("Opportunity email sent jobId={} freelancerId={} matchScore={}",
                    job.getId(), freelancer.getUserId(), matchScore);
        } catch (Exception e) {
            log.error("Opportunity email failed jobId={} freelancerId={}", job.getId(), freelancer.getUserId(), e);
        }
    }
}
