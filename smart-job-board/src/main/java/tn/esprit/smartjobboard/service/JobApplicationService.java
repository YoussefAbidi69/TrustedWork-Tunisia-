package tn.esprit.smartjobboard.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartjobboard.dto.ApplicationCreateRequest;
import tn.esprit.smartjobboard.dto.ApplicationStatusUpdateRequest;
import tn.esprit.smartjobboard.dto.JobApplicationResponse;
import tn.esprit.smartjobboard.dto.MatchScoreBreakdownDto;
import tn.esprit.smartjobboard.dto.UserReferenceDto;
import tn.esprit.smartjobboard.entity.ApplicationStatus;
import tn.esprit.smartjobboard.entity.FreelancerProfile;
import tn.esprit.smartjobboard.entity.JobApplication;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.entity.MatchScore;
import tn.esprit.smartjobboard.entity.SuccessPrediction;
import tn.esprit.smartjobboard.exception.DuplicateApplicationException;
import tn.esprit.smartjobboard.exception.ForbiddenOperationException;
import tn.esprit.smartjobboard.exception.InvalidStatusTransitionException;
import tn.esprit.smartjobboard.exception.JobClosedException;
import tn.esprit.smartjobboard.repository.FreelancerProfileRepository;
import tn.esprit.smartjobboard.repository.JobApplicationRepository;
import tn.esprit.smartjobboard.repository.JobOfferRepository;
import tn.esprit.smartjobboard.repository.MatchScoreRepository;
import tn.esprit.smartjobboard.repository.SuccessPredictionRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Applications, profile upserts for skills, client-side status transitions, and freelancer application listings with AI scores.
 */
@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final OpportunityScoreService opportunityScoreService;
    private final CurrentUserService currentUserService;
    private final MatchingEngineService matchingEngineService;
    private final MatchScoreRepository matchScoreRepository;
    private final SuccessPredictionRepository successPredictionRepository;

    @Transactional
    public JobApplicationResponse submit(ApplicationCreateRequest req) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        assertRole(me, "FREELANCER");

        JobOffer job = jobOfferRepository.findById(req.getJobOfferId())
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + req.getJobOfferId()));

        if (job.getStatus() == JobOfferStatus.CLOSED) {
            throw new JobClosedException("This job is closed");
        }
        if (job.getStatus() == JobOfferStatus.FLAGGED) {
            throw new JobClosedException("This job is under review");
        }
        if (job.getStatus() != JobOfferStatus.PUBLISHED) {
            throw new JobClosedException("This job is not open for applications");
        }

        if (jobApplicationRepository.existsByJobOfferIdAndFreelancerId(job.getId(), me.getId())) {
            throw new DuplicateApplicationException("You have already applied to this job");
        }

        upsertFreelancerProfile(me, req.getDeclaredSkills(), req.getProposedRate());

        JobApplication app = new JobApplication();
        app.setJobOffer(job);
        app.setFreelancerId(me.getId());
        app.setCoverLetter(req.getCoverLetter().trim());
        app.setProposedRate(req.getProposedRate());
        app.setStatus(ApplicationStatus.PENDING);
        JobApplication saved = jobApplicationRepository.save(app);

        opportunityScoreService.computeAndPersist(job);
        jobOfferRepository.save(job);

        FreelancerProfile fp = freelancerProfileRepository.findByUserId(me.getId())
                .orElseThrow(() -> new IllegalStateException("Freelancer profile missing after upsert."));
        MatchingEngineService.MatchComputationResult comp =
                matchingEngineService.computePersistAndReturn(job, fp, req.getProposedRate());

        return buildResponse(saved, breakdownFrom(comp.matchScore()),
                comp.successPrediction().getProbability(),
                comp.successPrediction().getConfidence().name());
    }

    @Transactional(readOnly = true)
    public List<JobApplicationResponse> listMineForFreelancer() {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        assertRole(me, "FREELANCER");
        return jobApplicationRepository.findMineWithJobOffer(me.getId()).stream()
                .sorted(Comparator.comparing(JobApplication::getAppliedAt).reversed())
                .map(this::buildFreelancerRow)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<JobApplicationResponse> listAllForAdmin(Pageable pageable,
                                                        ApplicationStatus statusFilter,
                                                        Double minMatchScore) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        assertRole(me, "ADMIN");
        Specification<JobApplication> spec = (root, query, cb) -> cb.conjunction();
        if (statusFilter != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), statusFilter));
        }
        if (minMatchScore != null && minMatchScore > 0) {
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> sq = query.subquery(Long.class);
                Root<MatchScore> ms = sq.from(MatchScore.class);
                sq.select(ms.get("id"));
                sq.where(
                        cb.equal(ms.get("jobOfferId"), root.get("jobOffer").get("id")),
                        cb.equal(ms.get("freelancerId"), root.get("freelancerId")),
                        cb.ge(ms.get("totalScore"), minMatchScore)
                );
                return cb.exists(sq);
            });
        }
        Page<JobApplication> page = jobApplicationRepository.findAll(spec, pageable);
        return page.map(a -> buildResponse(a, loadMatchBreakdown(a), loadSuccessProb(a), loadConfidence(a)));
    }

    @Transactional(readOnly = true)
    public List<JobApplicationResponse> listForJob(Long jobId) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        assertRole(me, "CLIENT");
        JobOffer job = jobOfferRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + jobId));
        if (!job.getClientId().equals(me.getId())) {
            throw new ForbiddenOperationException("You can only view applications for your own jobs.");
        }
        return jobApplicationRepository.findByJobOfferIdWithJob(jobId).stream()
                .sorted(Comparator.comparing(JobApplication::getAppliedAt).reversed())
                .map(a -> buildResponse(a, loadMatchBreakdown(a), loadSuccessProb(a), loadConfidence(a)))
                .collect(Collectors.toList());
    }

    @Transactional
    public JobApplicationResponse updateStatus(Long applicationId, ApplicationStatusUpdateRequest body) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        assertRole(me, "CLIENT");

        JobApplication app = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + applicationId));
        JobOffer job = app.getJobOffer();
        if (!job.getClientId().equals(me.getId())) {
            throw new ForbiddenOperationException("You can only update applications for your own jobs.");
        }

        ApplicationStatus next = body.getStatus();
        validateClientTransition(app.getStatus(), next);
        app.setStatus(next);
        JobApplication saved = jobApplicationRepository.save(app);
        return buildResponse(saved, loadMatchBreakdown(saved), loadSuccessProb(saved), loadConfidence(saved));
    }

    @Transactional
    public JobApplicationResponse withdraw(Long applicationId) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        assertRole(me, "FREELANCER");

        JobApplication app = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + applicationId));
        if (!app.getFreelancerId().equals(me.getId())) {
            throw new ForbiddenOperationException("You can only withdraw your own applications.");
        }
        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new InvalidStatusTransitionException("Only pending applications can be withdrawn.");
        }
        app.setStatus(ApplicationStatus.WITHDRAWN);
        JobApplication saved = jobApplicationRepository.save(app);
        opportunityScoreService.computeAndPersist(app.getJobOffer());
        jobOfferRepository.save(app.getJobOffer());
        return buildFreelancerRow(saved);
    }

    private JobApplicationResponse buildFreelancerRow(JobApplication app) {
        return buildResponse(app, loadMatchBreakdown(app), loadSuccessProb(app), loadConfidence(app));
    }

    private MatchScoreBreakdownDto loadMatchBreakdown(JobApplication app) {
        JobOffer j = app.getJobOffer();
        return matchScoreRepository.findByJobOfferIdAndFreelancerId(j.getId(), app.getFreelancerId())
                .map(this::breakdownFrom)
                .orElse(null);
    }

    private Double loadSuccessProb(JobApplication app) {
        JobOffer j = app.getJobOffer();
        return successPredictionRepository.findByJobOfferIdAndFreelancerId(j.getId(), app.getFreelancerId())
                .map(SuccessPrediction::getProbability)
                .orElse(null);
    }

    private String loadConfidence(JobApplication app) {
        JobOffer j = app.getJobOffer();
        return successPredictionRepository.findByJobOfferIdAndFreelancerId(j.getId(), app.getFreelancerId())
                .map(p -> p.getConfidence().name())
                .orElse(null);
    }

    private MatchScoreBreakdownDto breakdownFrom(MatchScore ms) {
        return MatchScoreBreakdownDto.builder()
                .skillMatch(ms.getSkillMatch())
                .reputation(ms.getReputation())
                .successRate(ms.getSuccessRate())
                .budgetFit(ms.getBudgetFit())
                .availability(ms.getAvailability())
                .totalScore(ms.getTotalScore())
                .build();
    }

    private JobApplicationResponse buildResponse(JobApplication app,
                                                 MatchScoreBreakdownDto match,
                                                 Double successProbability,
                                                 String predictionConfidence) {
        JobOffer j = app.getJobOffer();
        return JobApplicationResponse.builder()
                .id(app.getId())
                .jobOfferId(j.getId())
                .jobTitle(j.getTitle())
                .jobCategory(j.getCategory())
                .jobLocation(j.getLocation())
                .jobStatus(j.getStatus().name())
                .freelancerId(app.getFreelancerId())
                .coverLetter(app.getCoverLetter())
                .proposedRate(app.getProposedRate())
                .status(app.getStatus())
                .appliedAt(app.getAppliedAt())
                .matchScore(match)
                .successProbability(successProbability)
                .predictionConfidence(predictionConfidence)
                .build();
    }

    private void upsertFreelancerProfile(UserReferenceDto me, List<String> declaredSkills, java.math.BigDecimal proposedRate) {
        FreelancerProfile fp = freelancerProfileRepository.findByUserId(me.getId()).orElseGet(FreelancerProfile::new);
        fp.setUserId(me.getId());
        fp.setEmail(me.getEmail());
        fp.setPreferredRate(proposedRate);
        Set<String> merged = new LinkedHashSet<>();
        if (fp.getSkills() != null) {
            merged.addAll(fp.getSkills());
        }
        if (declaredSkills != null) {
            for (String s : declaredSkills) {
                if (s != null && !s.isBlank()) {
                    merged.add(s.trim());
                }
            }
        }
        fp.setSkills(new ArrayList<>(merged));
        freelancerProfileRepository.save(fp);
    }

    private static void validateClientTransition(ApplicationStatus from, ApplicationStatus to) {
        if (from == ApplicationStatus.WITHDRAWN || from == ApplicationStatus.REJECTED) {
            throw new InvalidStatusTransitionException("Terminal application status cannot be changed.");
        }
        if (from == ApplicationStatus.ACCEPTED) {
            throw new InvalidStatusTransitionException("Accepted applications cannot change status.");
        }
        switch (from) {
            case PENDING -> {
                if (to != ApplicationStatus.SHORTLISTED && to != ApplicationStatus.REJECTED && to != ApplicationStatus.ACCEPTED) {
                    throw new InvalidStatusTransitionException("Invalid transition from PENDING to " + to);
                }
            }
            case SHORTLISTED -> {
                if (to != ApplicationStatus.ACCEPTED && to != ApplicationStatus.REJECTED) {
                    throw new InvalidStatusTransitionException("Invalid transition from SHORTLISTED to " + to);
                }
            }
            default -> throw new InvalidStatusTransitionException("Unsupported transition from " + from);
        }
    }

    private static void assertRole(UserReferenceDto me, String role) {
        if (me.getRole() == null || !me.getRole().equalsIgnoreCase(role)) {
            throw new ForbiddenOperationException("This action requires role " + role + ".");
        }
    }
}
