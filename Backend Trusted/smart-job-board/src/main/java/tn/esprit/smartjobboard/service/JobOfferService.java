package tn.esprit.smartjobboard.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartjobboard.dto.FraudSignalDto;
import tn.esprit.smartjobboard.dto.JobOfferCreateRequest;
import tn.esprit.smartjobboard.dto.JobOfferResponse;
import tn.esprit.smartjobboard.dto.OfferFlagDto;
import tn.esprit.smartjobboard.dto.JobOfferUpdateRequest;
import tn.esprit.smartjobboard.dto.PreviewSkillsRequest;
import tn.esprit.smartjobboard.dto.PreviewSkillsResponse;
import tn.esprit.smartjobboard.dto.MatchFreelancerRowDto;
import tn.esprit.smartjobboard.dto.SuccessPredictionViewDto;
import tn.esprit.smartjobboard.dto.UserReferenceDto;
import tn.esprit.smartjobboard.entity.FreelancerProfile;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.entity.OfferFlag;
import tn.esprit.smartjobboard.exception.ForbiddenOperationException;
import tn.esprit.smartjobboard.exception.JobNotEditableException;
import tn.esprit.smartjobboard.repository.BudgetIntelligenceRepository;
import tn.esprit.smartjobboard.repository.CompatibilityReportRepository;
import tn.esprit.smartjobboard.repository.FreelancerProfileRepository;
import tn.esprit.smartjobboard.repository.JobApplicationRepository;
import tn.esprit.smartjobboard.repository.JobDemandSnapshotRepository;
import tn.esprit.smartjobboard.repository.JobOfferRepository;
import tn.esprit.smartjobboard.repository.MatchScoreRepository;
import tn.esprit.smartjobboard.repository.OfferFlagRepository;
import tn.esprit.smartjobboard.repository.OpportunityNotificationLogRepository;
import tn.esprit.smartjobboard.repository.SuccessPredictionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Job lifecycle, fraud/opportunity hooks, visibility rules, and client-scoped access control.
 */
@Service
@RequiredArgsConstructor
public class JobOfferService {

    private final JobOfferRepository jobOfferRepository;
    private final OfferFlagRepository offerFlagRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final MatchScoreRepository matchScoreRepository;
    private final CompatibilityReportRepository compatibilityReportRepository;
    private final SuccessPredictionRepository successPredictionRepository;
    private final BudgetIntelligenceRepository budgetIntelligenceRepository;
    private final JobDemandSnapshotRepository jobDemandSnapshotRepository;
    private final OpportunityNotificationLogRepository opportunityNotificationLogRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final SkillExtractionService skillExtractionService;
    private final FraudDetectionService fraudDetectionService;
    private final OpportunityScoreService opportunityScoreService;
    private final MatchingEngineService matchingEngineService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public PreviewSkillsResponse previewSkills(PreviewSkillsRequest req) {
        return new PreviewSkillsResponse(skillExtractionService.extractFromDescription(req.getDescription()));
    }

    @Transactional
    public JobOfferResponse create(JobOfferCreateRequest req) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        assertRole(me, "CLIENT");

        validateBudget(req.getBudgetMin(), req.getBudgetMax());

        JobOffer job = new JobOffer();
        job.setClientId(me.getId());
        mapCreate(req, job);
        job.setExtractedSkills(skillExtractionService.extractFromDescription(job.getDescription()));
        job.setStatus(JobOfferStatus.DRAFT);

        JobOffer saved = jobOfferRepository.save(job);
        applyFraudAndOpportunity(saved);
        return toResponse(jobOfferRepository.save(saved));
    }

    @Transactional
    public JobOfferResponse update(Long id, JobOfferUpdateRequest req) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        assertRole(me, "CLIENT");

        JobOffer job = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + id));
        if (!job.getClientId().equals(me.getId())) {
            throw new ForbiddenOperationException("You can only update your own job offers.");
        }
        if (job.getStatus() != JobOfferStatus.DRAFT) {
            throw new JobNotEditableException("Published jobs cannot be edited");
        }

        validateBudget(req.getBudgetMin(), req.getBudgetMax());
        mapUpdate(req, job);
        job.setExtractedSkills(skillExtractionService.extractFromDescription(job.getDescription()));

        JobOffer saved = jobOfferRepository.save(job);
        applyFraudAndOpportunity(saved);
        return toResponse(jobOfferRepository.save(saved));
    }

    @Transactional(readOnly = true)
    public JobOfferResponse get(Long id) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        JobOffer job = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + id));
        if (!canRead(job, me)) {
            throw new ForbiddenOperationException("You are not allowed to view this job offer.");
        }
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public SuccessPredictionViewDto getSuccessPrediction(Long jobId, Long freelancerId) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        JobOffer job = jobOfferRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + jobId));
        boolean clientOwner = me.getRole() != null && me.getRole().equalsIgnoreCase("CLIENT")
                && job.getClientId().equals(me.getId());
        boolean freelancerSelf = me.getRole() != null && me.getRole().equalsIgnoreCase("FREELANCER")
                && freelancerId.equals(me.getId());
        if (!clientOwner && !freelancerSelf) {
            throw new ForbiddenOperationException("You cannot view this prediction.");
        }
        return successPredictionRepository.findByJobOfferIdAndFreelancerId(jobId, freelancerId)
                .map(sp -> buildSuccessPredictionView(job, freelancerId, sp.getProbability(), sp.getConfidence().name()))
                .orElseGet(() -> buildSuccessPredictionView(job, freelancerId, 0.0, "LOW"));
    }

    @Transactional(readOnly = true)
    public JobOfferResponse asResponse(JobOffer job) {
        return toResponse(job);
    }

    private SuccessPredictionViewDto buildSuccessPredictionView(JobOffer job, Long freelancerId,
                                                                double probability, String confidenceLabel) {
        FreelancerProfile fp = freelancerProfileRepository.findByUserId(freelancerId).orElseGet(FreelancerProfile::new);
        double overlap = computeSkillOverlapPercent(job, fp);
        double reputationScore = 70.0;
        double successRateScore = 75.0;
        return SuccessPredictionViewDto.builder()
                .probability(probability)
                .confidenceLabel(confidenceLabel != null ? confidenceLabel : "LOW")
                .skillOverlapPercent(overlap)
                .reputationScore(reputationScore)
                .successRateScore(successRateScore)
                .predictionSummary(predictionSummaryFor(probability))
                .build();
    }

    private static String predictionSummaryFor(double probability) {
        if (probability > 0.7) {
            return "Strong match — your skills closely align with the job requirements and your track record suggests high success.";
        }
        if (probability >= 0.4) {
            return "Moderate fit — you meet several key requirements but there are skill gaps that may affect delivery.";
        }
        return "Low compatibility — significant skill gaps detected. Consider upskilling before applying.";
    }

    private double computeSkillOverlapPercent(JobOffer job, FreelancerProfile fp) {
        List<String> need = matchingEngineService.mergeJobSkills(job);
        if (need.isEmpty()) {
            return 0.0;
        }
        List<String> have = fp.getSkills() == null ? List.of() : fp.getSkills();
        Set<String> matched = new HashSet<>();
        for (String n : need) {
            if (n == null || n.isBlank()) {
                continue;
            }
            String nl = n.toLowerCase(Locale.ROOT);
            for (String h : have) {
                if (h != null && nl.equals(h.trim().toLowerCase(Locale.ROOT))) {
                    matched.add(nl);
                    break;
                }
            }
        }
        return Math.round(1000.0 * matched.size() / need.size()) / 10.0;
    }

    @Transactional(readOnly = true)
    public Page<JobOfferResponse> publicFeed(Pageable pageable) {
        return jobOfferRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("status"), JobOfferStatus.PUBLISHED),
                pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<JobOfferResponse> search(String category,
                                       List<String> skills,
                                       BigDecimal budgetMin,
                                       BigDecimal budgetMax,
                                       String location,
                                       Boolean remote,
                                       Boolean mine,
                                       Pageable pageable) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        Specification<JobOffer> spec = Specification.where(
                JobOfferSpecifications.visibility(me.getId(), me.getRole(), mine)
        ).and(JobOfferSpecifications.categoryEquals(category))
                .and(JobOfferSpecifications.anySkillMatches(skills))
                .and(JobOfferSpecifications.budgetOverlap(budgetMin, budgetMax))
                .and(JobOfferSpecifications.locationContains(location))
                .and(JobOfferSpecifications.remoteEquals(remote));

        return jobOfferRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional
    public void delete(Long id) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        assertRole(me, "CLIENT");
        JobOffer job = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + id));
        if (!job.getClientId().equals(me.getId())) {
            throw new ForbiddenOperationException("You can only delete your own job offers.");
        }
        if (job.getStatus() != JobOfferStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT job offers can be deleted.");
        }
        Long jid = job.getId();
        opportunityNotificationLogRepository.deleteByJobOfferId(jid);
        jobApplicationRepository.deleteByJobOfferId(jid);
        offerFlagRepository.deleteByJobOfferId(jid);
        compatibilityReportRepository.purgeByJobOfferId(jid);
        matchScoreRepository.deleteByJobOfferId(jid);
        successPredictionRepository.deleteByJobOfferId(jid);
        budgetIntelligenceRepository.deleteByJobOfferId(jid);
        jobDemandSnapshotRepository.deleteByJobOfferId(jid);
        jobOfferRepository.delete(job);
    }

    @Transactional
    public JobOfferResponse publish(Long id) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        assertRole(me, "CLIENT");
        JobOffer job = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + id));
        if (!job.getClientId().equals(me.getId())) {
            throw new ForbiddenOperationException("You can only publish your own job offers.");
        }
        if (job.getStatus() != JobOfferStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT job offers can be published.");
        }
        applyFraudAndOpportunity(job);
        if (job.getFraudRiskScore() >= 0.6 || job.getStatus() == JobOfferStatus.FLAGGED) {
            throw new IllegalArgumentException("Cannot publish: fraud risk is too high or the offer is flagged.");
        }
        job.setStatus(JobOfferStatus.PUBLISHED);
        job.setPublishedAt(LocalDateTime.now());
        job.setOpportunityAgentProcessedAt(null);
        opportunityScoreService.computeAndPersist(job);
        return toResponse(jobOfferRepository.save(job));
    }

    @Transactional
    public JobOfferResponse close(Long id) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        assertRole(me, "CLIENT");
        JobOffer job = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + id));
        if (!job.getClientId().equals(me.getId())) {
            throw new ForbiddenOperationException("You can only close your own job offers.");
        }
        if (job.getStatus() != JobOfferStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only PUBLISHED job offers can be closed.");
        }
        job.setStatus(JobOfferStatus.CLOSED);
        return toResponse(jobOfferRepository.save(job));
    }

    @Transactional
    public List<MatchFreelancerRowDto> matchesForJob(Long jobId) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        assertRole(me, "CLIENT");
        JobOffer job = jobOfferRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + jobId));
        if (!job.getClientId().equals(me.getId())) {
            throw new ForbiddenOperationException("You can only view matches for your own jobs.");
        }

        List<FreelancerProfile> profiles = freelancerProfileRepository.findAllWithSkills();
        List<MatchFreelancerRowDto> rows = new ArrayList<>();
        for (FreelancerProfile fp : profiles) {
            BigDecimal rate = rateForMatch(job, fp);
            MatchingEngineService.RawMatchEvaluation raw = matchingEngineService.evaluateRaw(job, fp, rate);
            rows.add(MatchFreelancerRowDto.builder()
                    .freelancerId(fp.getUserId())
                    .email(fp.getEmail())
                    .totalMatchScore(raw.totalScore())
                    .skillMatch(raw.skillMatch())
                    .reputation(raw.reputation())
                    .successRate(raw.successRate())
                    .budgetFit(raw.budgetFit())
                    .availability(raw.availability())
                    .successProbability(raw.successProbability())
                    .predictionConfidence(raw.confidence().name())
                    .build());
            matchingEngineService.persistRaw(job, fp, raw);
        }
        rows.sort(Comparator.comparingDouble(MatchFreelancerRowDto::getTotalMatchScore).reversed());
        return rows;
    }

    @Transactional(readOnly = true)
    public Page<JobOfferResponse> adminListAll(Pageable pageable, String status, String category, String search) {
        Specification<JobOffer> spec = (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                try {
                    JobOfferStatus st = JobOfferStatus.valueOf(status.trim().toUpperCase());
                    parts.add(cb.equal(root.get("status"), st));
                } catch (IllegalArgumentException ignored) {
                    // invalid filter value → return unfiltered list
                }
            }
            if (category != null && !category.isBlank()) {
                parts.add(cb.equal(root.get("category"), category.trim()));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                parts.add(cb.like(cb.lower(root.get("title")), like));
            }
            if (parts.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
        return jobOfferRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<JobOfferResponse> adminListFlagged(Pageable pageable) {
        return jobOfferRepository.findAll(
                (root, q, cb) -> cb.equal(root.get("status"), JobOfferStatus.FLAGGED),
                pageable
        ).map(this::toResponse);
    }

    @Transactional
    public JobOfferResponse adminFlag(Long id) {
        JobOffer job = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + id));
        job.setStatus(JobOfferStatus.FLAGGED);
        return toResponse(jobOfferRepository.save(job));
    }

    @Transactional
    public JobOfferResponse adminUnflag(Long id) {
        JobOffer job = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + id));
        if (job.getStatus() != JobOfferStatus.FLAGGED) {
            throw new IllegalArgumentException("Only flagged job offers can be unflagged.");
        }
        offerFlagRepository.deleteByJobOfferId(job.getId());
        job.setFraudRiskScore(0.0);
        job.setStatus(JobOfferStatus.PUBLISHED);
        if (job.getPublishedAt() == null) {
            job.setPublishedAt(LocalDateTime.now());
        }
        opportunityScoreService.computeAndPersist(job);
        return toResponse(jobOfferRepository.save(job));
    }

    @Transactional
    public JobOfferResponse adminForceClose(Long id) {
        JobOffer job = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + id));
        job.setStatus(JobOfferStatus.CLOSED);
        opportunityScoreService.computeAndPersist(job);
        return toResponse(jobOfferRepository.save(job));
    }

    /**
     * Admin update: any status, no client ownership check. Re-runs skill extraction and fraud/opportunity.
     */
    @Transactional
    public JobOfferResponse adminUpdateJob(Long id, JobOfferUpdateRequest req) {
        JobOffer job = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + id));
        validateBudget(req.getBudgetMin(), req.getBudgetMax());
        mapUpdate(req, job);
        job.setExtractedSkills(skillExtractionService.extractFromDescription(job.getDescription()));
        JobOffer saved = jobOfferRepository.save(job);
        applyFraudAndOpportunity(saved);
        return toResponse(jobOfferRepository.save(saved));
    }

    /**
     * Admin hard delete: cascades like client draft delete, without ownership or status restrictions.
     */
    @Transactional
    public void adminDeleteJob(Long id) {
        JobOffer job = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + id));
        Long jid = job.getId();
        opportunityNotificationLogRepository.deleteByJobOfferId(jid);
        jobApplicationRepository.deleteByJobOfferId(jid);
        offerFlagRepository.deleteByJobOfferId(jid);
        compatibilityReportRepository.purgeByJobOfferId(jid);
        matchScoreRepository.deleteByJobOfferId(jid);
        successPredictionRepository.deleteByJobOfferId(jid);
        budgetIntelligenceRepository.deleteByJobOfferId(jid);
        jobDemandSnapshotRepository.deleteByJobOfferId(jid);
        jobOfferRepository.delete(job);
    }

    @Transactional(readOnly = true)
    public List<OfferFlagDto> adminListFlagsForJob(Long jobOfferId) {
        JobOffer job = jobOfferRepository.findById(jobOfferId)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + jobOfferId));
        return offerFlagRepository.findByJobOffer_IdOrderByIdAsc(job.getId()).stream()
                .map(f -> OfferFlagDto.builder()
                        .id(f.getId())
                        .jobOfferId(job.getId())
                        .signalName(f.getSignalCode())
                        .signalWeight(f.getWeight())
                        .description(f.getMessage())
                        .detectedAt(f.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private BigDecimal rateForMatch(JobOffer job, FreelancerProfile fp) {
        if (fp.getPreferredRate() != null) {
            return fp.getPreferredRate();
        }
        return job.getBudgetMin().add(job.getBudgetMax())
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private boolean canRead(JobOffer job, UserReferenceDto me) {
        if ("ADMIN".equalsIgnoreCase(me.getRole())) {
            return true;
        }
        if (job.getClientId().equals(me.getId())) {
            return true;
        }
        return job.getStatus() == JobOfferStatus.PUBLISHED;
    }

    private void applyFraudAndOpportunity(JobOffer job) {
        offerFlagRepository.deleteByJobOfferId(job.getId());
        FraudDetectionService.FraudAssessment fa = fraudDetectionService.assess(job, job.getId());
        job.setFraudRiskScore(fa.fraudRiskScore());
        for (FraudDetectionService.TriggeredSignal t : fa.triggers()) {
            OfferFlag flag = new OfferFlag();
            flag.setJobOffer(job);
            flag.setSignalCode(t.code());
            flag.setMessage(t.message());
            flag.setWeight(t.weight());
            offerFlagRepository.save(flag);
        }
        if (fa.fraudRiskScore() >= 0.6) {
            job.setStatus(JobOfferStatus.FLAGGED);
        } else if (job.getStatus() == JobOfferStatus.PUBLISHED || job.getStatus() == JobOfferStatus.CLOSED) {
            // lifecycle states are not auto-downgraded here
        } else if (job.getStatus() == JobOfferStatus.FLAGGED) {
            job.setStatus(JobOfferStatus.DRAFT);
        } else {
            job.setStatus(JobOfferStatus.DRAFT);
        }
        opportunityScoreService.computeAndPersist(job);
    }

    private void mapCreate(JobOfferCreateRequest req, JobOffer job) {
        job.setTitle(req.getTitle().trim());
        job.setDescription(req.getDescription().trim());
        job.setCategory(req.getCategory().trim());
        job.setRequiredSkills(cleanSkills(req.getRequiredSkills()));
        job.setBudgetMin(req.getBudgetMin());
        job.setBudgetMax(req.getBudgetMax());
        job.setDurationDays(req.getDurationDays());
        job.setLocation(req.getLocation() != null ? req.getLocation().trim() : null);
        job.setRemote(req.isRemote());
        job.setExpiresAt(req.getExpiresAt());
    }

    private void mapUpdate(JobOfferUpdateRequest req, JobOffer job) {
        job.setTitle(req.getTitle().trim());
        job.setDescription(req.getDescription().trim());
        job.setCategory(req.getCategory().trim());
        job.setRequiredSkills(cleanSkills(req.getRequiredSkills()));
        job.setBudgetMin(req.getBudgetMin());
        job.setBudgetMax(req.getBudgetMax());
        job.setDurationDays(req.getDurationDays());
        job.setLocation(req.getLocation() != null ? req.getLocation().trim() : null);
        job.setRemote(req.isRemote());
        job.setExpiresAt(req.getExpiresAt());
    }

    private static List<String> cleanSkills(List<String> skills) {
        if (skills == null) {
            return new ArrayList<>();
        }
        return skills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    private static void validateBudget(BigDecimal min, BigDecimal max) {
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("budgetMin cannot be greater than budgetMax.");
        }
    }

    private JobOfferResponse toResponse(JobOffer job) {
        List<FraudSignalDto> signals = offerFlagRepository.findByJobOffer_IdOrderByIdAsc(job.getId()).stream()
                .map(f -> new FraudSignalDto(f.getSignalCode(), f.getMessage(), f.getWeight()))
                .collect(Collectors.toList());

        return JobOfferResponse.builder()
                .id(job.getId())
                .clientId(job.getClientId())
                .title(job.getTitle())
                .description(job.getDescription())
                .category(job.getCategory())
                .requiredSkills(new ArrayList<>(job.getRequiredSkills()))
                .extractedSkills(new ArrayList<>(job.getExtractedSkills()))
                .budgetMin(job.getBudgetMin())
                .budgetMax(job.getBudgetMax())
                .durationDays(job.getDurationDays())
                .location(job.getLocation())
                .remote(job.isRemote())
                .status(job.getStatus())
                .fraudRiskScore(job.getFraudRiskScore())
                .opportunityScore(job.getOpportunityScore())
                .opportunityBudgetComponent(job.getOpportunityBudgetComponent())
                .opportunityDemandComponent(job.getOpportunityDemandComponent())
                .opportunityCompetitionComponent(job.getOpportunityCompetitionComponent())
                .publishedAt(job.getPublishedAt())
                .expiresAt(job.getExpiresAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .fraudSignals(signals)
                .applicationCount(jobApplicationRepository.countByJobOfferId(job.getId()))
                .build();
    }

    private static void assertRole(UserReferenceDto me, String role) {
        if (me.getRole() == null || !me.getRole().equalsIgnoreCase(role)) {
            throw new ForbiddenOperationException("This action requires role " + role + ".");
        }
    }
}
