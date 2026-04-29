package tn.esprit.smartjobboard.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartjobboard.entity.ApplicationStatus;
import tn.esprit.smartjobboard.entity.FreelancerProfile;
import tn.esprit.smartjobboard.entity.JobApplication;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.entity.OfferFlag;
import tn.esprit.smartjobboard.repository.FreelancerProfileRepository;
import tn.esprit.smartjobboard.repository.JobApplicationRepository;
import tn.esprit.smartjobboard.repository.JobOfferRepository;
import tn.esprit.smartjobboard.repository.OfferFlagRepository;
import tn.esprit.smartjobboard.service.MatchingEngineService;
import tn.esprit.smartjobboard.service.OpportunityScoreService;
import tn.esprit.smartjobboard.service.SkillCooccurrenceService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Inserts realistic demo jobs, applications, freelancer profiles, and fraud flags when the DB is empty
 * and {@code jobboard.demo-data.enabled=true}. Adjust user ids in configuration to match your user-service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "jobboard.demo-data.enabled", havingValue = "true")
public class JobBoardDemoDataLoader {

    private final JobOfferRepository jobOfferRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final OfferFlagRepository offerFlagRepository;
    private final MatchingEngineService matchingEngineService;
    private final OpportunityScoreService opportunityScoreService;
    private final SkillCooccurrenceService skillCooccurrenceService;

    @Value("${jobboard.demo-data.client-user-id:2}")
    private long clientUserId;

    @Value("${jobboard.demo-data.freelancer-user-ids:3,4}")
    private String freelancerUserIdsCsv;

    @Transactional
    public void loadIfEmpty() {
        if (jobOfferRepository.count() > 0) {
            log.info("Job board already contains job offers; demo data loader skipped.");
            return;
        }

        List<Long> freelancerIds = new ArrayList<>(parseIds(freelancerUserIdsCsv));
        if (freelancerIds.size() < 2) {
            log.warn("jobboard.demo-data.freelancer-user-ids should list at least two user ids; seeding with defaults 3 and 4.");
            freelancerIds.clear();
            freelancerIds.add(3L);
            freelancerIds.add(4L);
        }
        long f1 = freelancerIds.get(0);
        long f2 = freelancerIds.get(1);

        upsertProfile(f1, "freelancer.demo1@trustedwork.tn", List.of("Java", "Spring Boot", "REST", "MySQL", "Docker"), bd("950"));
        upsertProfile(f2, "freelancer.demo2@trustedwork.tn", List.of("Angular", "TypeScript", "UI/UX", "Figma", "Java"), bd("1100"));

        LocalDateTime now = LocalDateTime.now();

        JobOffer j1 = publishJob("Senior Backend Engineer — Spring & microservices",
                longDesc("Design and implement REST APIs for our marketplace core. Experience with Spring Boot 3, JPA, and event-driven patterns."),
                "Software", List.of("Java", "Spring Boot", "REST", "MySQL"), List.of("Kafka", "Docker", "JUnit"),
                bd("1200"), bd("3200"), 60, "Tunis", true, 0.12, now.minusDays(8), clientUserId);

        JobOffer j2 = publishJob("Product Designer — SaaS dashboards",
                longDesc("Own end-to-end UX for analytics dashboards. Strong portfolio in B2B SaaS and design systems required."),
                "Design", List.of("Figma", "UI/UX", "Design systems"), List.of("Prototyping", "Accessibility"),
                bd("900"), bd("2200"), 45, "Sfax", false, 0.38, now.minusDays(12), clientUserId);

        JobOffer j3 = publishJob("Full-stack developer — remote friendly",
                longDesc("Join a distributed team building Angular + Spring apps. Must be comfortable with CI/CD and code reviews."),
                "Software", List.of("Angular", "Java", "TypeScript"), List.of("Spring", "GitLab CI"),
                bd("1500"), bd("4000"), 90, "Remote", true, 0.22, now.minusDays(45), clientUserId);

        JobOffer j4 = publishJob("Urgent: data scraping & automation (high volume)",
                longDesc("We need scripts to harvest public listings quickly. Pay per milestone. Telegram contact preferred."),
                "Data", List.of("Python", "Automation"), List.of("Scraping", "Bypass"),
                bd("100"), bd("400"), 7, "Unspecified", true, 0.78, now.minusDays(3), clientUserId);

        JobOffer j5 = publishJob("Growth marketer — paid social",
                longDesc("Plan and run Meta & LinkedIn campaigns for lead generation in the MENA region. Arabic copy a plus."),
                "Marketing", List.of("Meta Ads", "Copywriting", "Analytics"), List.of("LinkedIn", "GA4"),
                bd("800"), bd("2000"), 30, "Tunis", false, 0.28, now.minusDays(20), clientUserId);

        JobOffer j6 = draftJob("Mobile engineer — Flutter (draft)",
                longDesc("Retail client wants a white-label loyalty app. Draft posting for internal review."),
                "Software", List.of("Flutter", "Dart", "Firebase"), List.of("iOS", "Android"),
                bd("1300"), bd("3500"), 75, "Tunis", true, clientUserId);

        JobOffer j7 = draftJob("Technical writer — API docs (draft)",
                longDesc("Produce OpenAPI-first documentation and developer guides for our public API."),
                "Operations", List.of("Technical writing", "OpenAPI"), List.of("Markdown", "Postman"),
                bd("600"), bd("1400"), 21, "Remote", true, clientUserId);

        JobOffer j8 = closedJob("Legacy PHP maintenance (closed)",
                longDesc("Short engagement to stabilise legacy billing module. Position has been filled."),
                "Software", List.of("PHP", "MySQL"), List.of("Laravel"),
                bd("500"), bd("1200"), 14, "Sousse", false, now.minusDays(60), clientUserId);

        List<JobOffer> saved = jobOfferRepository.saveAll(List.of(j1, j2, j3, j4, j5, j6, j7, j8));
        JobOffer savedJ1 = findByTitle(saved, j1.getTitle());
        JobOffer savedJ2 = findByTitle(saved, j2.getTitle());
        JobOffer savedJ3 = findByTitle(saved, j3.getTitle());
        JobOffer savedJ4 = findByTitle(saved, j4.getTitle());

        addFlag(savedJ4, "SUSPICIOUS_BUDGET", "Budget far below typical market range for stated scope.", 0.35);
        addFlag(savedJ4, "CONTACT_REDIRECT", "Description requests off-platform messaging.", 0.42);

        saveApp(savedJ1, f1, ApplicationStatus.PENDING, bd("1400"));
        saveApp(savedJ1, f2, ApplicationStatus.SHORTLISTED, bd("1600"));
        saveApp(savedJ2, f1, ApplicationStatus.ACCEPTED, bd("1100"));
        saveApp(savedJ4, f1, ApplicationStatus.PENDING, bd("250"));

        for (JobApplication a : jobApplicationRepository.findByJobOfferIdWithJob(savedJ1.getId())) {
            runMatch(a);
        }
        for (JobApplication a : jobApplicationRepository.findByJobOfferIdWithJob(savedJ2.getId())) {
            runMatch(a);
        }
        for (JobApplication a : jobApplicationRepository.findByJobOfferIdWithJob(savedJ4.getId())) {
            runMatch(a);
        }

        for (JobOffer j : jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED)) {
            opportunityScoreService.computeAndPersist(j);
            jobOfferRepository.save(j);
        }

        skillCooccurrenceService.rebuildFromPublishedJobs();

        log.info("Demo job board data loaded (clientUserId={}, freelancers={}).", clientUserId, freelancerIds);
    }

    private void runMatch(JobApplication a) {
        JobOffer job = jobOfferRepository.findById(a.getJobOffer().getId()).orElseThrow();
        FreelancerProfile fp = freelancerProfileRepository.findByUserId(a.getFreelancerId()).orElseThrow();
        matchingEngineService.computePersistAndReturn(job, fp, a.getProposedRate());
    }

    private static JobOffer findByTitle(List<JobOffer> saved, String title) {
        return saved.stream().filter(j -> title.equals(j.getTitle())).findFirst().orElseThrow();
    }

    private void saveApp(JobOffer job, long freelancerId, ApplicationStatus status, BigDecimal rate) {
        JobApplication a = new JobApplication();
        a.setJobOffer(job);
        a.setFreelancerId(freelancerId);
        a.setCoverLetter(
                "I am interested in this role and have relevant experience across the stack you described. "
                        + "I can start within two weeks and collaborate closely with your team on delivery milestones.");
        a.setProposedRate(rate);
        a.setStatus(status);
        jobApplicationRepository.save(a);
    }

    private void addFlag(JobOffer job, String code, String message, double weight) {
        OfferFlag f = new OfferFlag();
        f.setJobOffer(job);
        f.setSignalCode(code);
        f.setMessage(message);
        f.setWeight(weight);
        offerFlagRepository.save(f);
    }

    private void upsertProfile(long userId, String email, List<String> skills, BigDecimal rate) {
        FreelancerProfile fp = freelancerProfileRepository.findByUserId(userId).orElseGet(FreelancerProfile::new);
        fp.setUserId(userId);
        fp.setEmail(email);
        fp.setSkills(new ArrayList<>(skills));
        fp.setPreferredRate(rate);
        freelancerProfileRepository.save(fp);
    }

    private JobOffer publishJob(String title, String desc, String category,
                                List<String> required, List<String> extracted,
                                BigDecimal min, BigDecimal max, int duration, String loc, boolean remote,
                                double fraudRisk, LocalDateTime publishedAt, long clientId) {
        JobOffer j = baseJob(title, desc, category, required, extracted, min, max, duration, loc, remote, clientId);
        j.setStatus(JobOfferStatus.PUBLISHED);
        j.setFraudRiskScore(fraudRisk);
        j.setPublishedAt(publishedAt);
        j.setExpiresAt(LocalDateTime.now().plusDays(45));
        return j;
    }

    private JobOffer draftJob(String title, String desc, String category,
                              List<String> required, List<String> extracted,
                              BigDecimal min, BigDecimal max, int duration, String loc, boolean remote, long clientId) {
        JobOffer j = baseJob(title, desc, category, required, extracted, min, max, duration, loc, remote, clientId);
        j.setStatus(JobOfferStatus.DRAFT);
        j.setFraudRiskScore(0.08);
        j.setOpportunityScore(55);
        j.setOpportunityBudgetComponent(50.0);
        j.setOpportunityDemandComponent(58.0);
        j.setOpportunityCompetitionComponent(52.0);
        return j;
    }

    private JobOffer closedJob(String title, String desc, String category,
                               List<String> required, List<String> extracted,
                               BigDecimal min, BigDecimal max, int duration, String loc, boolean remote,
                               LocalDateTime publishedAt, long clientId) {
        JobOffer j = baseJob(title, desc, category, required, extracted, min, max, duration, loc, remote, clientId);
        j.setStatus(JobOfferStatus.CLOSED);
        j.setFraudRiskScore(0.18);
        j.setPublishedAt(publishedAt);
        j.setExpiresAt(LocalDateTime.now().plusDays(10));
        return j;
    }

    private static JobOffer baseJob(String title, String desc, String category,
                                     List<String> required, List<String> extracted,
                                     BigDecimal min, BigDecimal max, int duration, String loc, boolean remote,
                                     long clientId) {
        JobOffer j = new JobOffer();
        j.setClientId(clientId);
        j.setTitle(title);
        j.setDescription(desc);
        j.setCategory(category);
        j.getRequiredSkills().addAll(required);
        j.getExtractedSkills().addAll(extracted);
        j.setBudgetMin(min);
        j.setBudgetMax(max);
        j.setDurationDays(duration);
        j.setLocation(loc);
        j.setRemote(remote);
        j.setOpportunityScore(65);
        j.setOpportunityBudgetComponent(62.0);
        j.setOpportunityDemandComponent(68.0);
        j.setOpportunityCompetitionComponent(60.0);
        return j;
    }

    private static String longDesc(String core) {
        return core + " "
                + "You will collaborate with product and engineering leads, participate in agile ceremonies, "
                + "and deliver production-quality increments every sprint. Clear communication and documentation are expected.";
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static List<Long> parseIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList());
    }
}
