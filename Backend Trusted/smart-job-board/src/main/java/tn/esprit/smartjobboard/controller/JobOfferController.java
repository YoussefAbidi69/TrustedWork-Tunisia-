package tn.esprit.smartjobboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.smartjobboard.dto.JobOfferCreateRequest;
import tn.esprit.smartjobboard.dto.JobOfferResponse;
import tn.esprit.smartjobboard.dto.JobOfferUpdateRequest;
import tn.esprit.smartjobboard.dto.JobApplicationResponse;
import tn.esprit.smartjobboard.dto.ApplicationCreateRequest;
import tn.esprit.smartjobboard.dto.PreviewSkillsRequest;
import tn.esprit.smartjobboard.dto.PreviewSkillsResponse;
import tn.esprit.smartjobboard.dto.MatchFreelancerRowDto;
import tn.esprit.smartjobboard.dto.SuccessPredictionViewDto;
import tn.esprit.smartjobboard.service.JobApplicationService;
import tn.esprit.smartjobboard.service.JobOfferService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Tag(name = "Job offers")
public class JobOfferController {

    private final JobOfferService jobOfferService;
    private final JobApplicationService jobApplicationService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Create draft job offer")
    public ResponseEntity<JobOfferResponse> create(@Valid @RequestBody JobOfferCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobOfferService.create(request));
    }

    @PostMapping("/preview-skills")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "AI skill extraction preview from a job description fragment")
    public ResponseEntity<PreviewSkillsResponse> previewSkills(@Valid @RequestBody PreviewSkillsRequest request) {
        return ResponseEntity.ok(jobOfferService.previewSkills(request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "List authenticated client's job offers (same as GET /jobs?mine=true)")
    public ResponseEntity<Page<JobOfferResponse>> myJobs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(jobOfferService.search(null, null, null, null, null, null, true, pageable));
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Search job offers (published, or your own when mine=true)")
    public ResponseEntity<Page<JobOfferResponse>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) BigDecimal budgetMin,
            @RequestParam(required = false) BigDecimal budgetMax,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(required = false) Boolean mine,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (mine == null && category == null && skills == null && budgetMin == null && budgetMax == null && location == null && remote == null) {
            return ResponseEntity.ok(jobOfferService.publicFeed(pageable));
        }
        return ResponseEntity.ok(jobOfferService.search(category, skills, budgetMin, budgetMax, location, remote, mine, pageable));
    }

    @GetMapping("/{id}/extracted-skills")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PreviewSkillsResponse> extractedSkills(@PathVariable Long id) {
        JobOfferResponse job = jobOfferService.get(id);
        return ResponseEntity.ok(new PreviewSkillsResponse(job.getExtractedSkills()));
    }

    @GetMapping("/{id}/success-prediction")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Success prediction for a freelancer on this job (job owner or same freelancer)")
    public ResponseEntity<SuccessPredictionViewDto> successPrediction(
            @PathVariable Long id,
            @RequestParam Long freelancerId
    ) {
        return ResponseEntity.ok(jobOfferService.getSuccessPrediction(id, freelancerId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get job offer by id")
    public ResponseEntity<JobOfferResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(jobOfferService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Update draft job offer")
    public ResponseEntity<JobOfferResponse> update(@PathVariable Long id, @Valid @RequestBody JobOfferUpdateRequest request) {
        return ResponseEntity.ok(jobOfferService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Delete draft job offer")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobOfferService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Publish draft job offer")
    public ResponseEntity<JobOfferResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(jobOfferService.publish(id));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Close published job offer")
    public ResponseEntity<JobOfferResponse> close(@PathVariable Long id) {
        return ResponseEntity.ok(jobOfferService.close(id));
    }

    @GetMapping("/{id}/matches")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Ranked freelancers for this job")
    public ResponseEntity<List<MatchFreelancerRowDto>> matches(@PathVariable Long id) {
        return ResponseEntity.ok(jobOfferService.matchesForJob(id));
    }

    @GetMapping("/{id}/applications")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Applications for this job (owner only)")
    public ResponseEntity<List<JobApplicationResponse>> applications(@PathVariable Long id) {
        return ResponseEntity.ok(jobApplicationService.listForJob(id));
    }

    @PostMapping("/{id}/apply")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<JobApplicationResponse> apply(@PathVariable Long id, @RequestBody ApplicationCreateRequest request) {
        request.setJobOfferId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(jobApplicationService.submit(request));
    }

    @GetMapping("/{id}/match")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MatchFreelancerRowDto>> match(@PathVariable Long id) {
        return ResponseEntity.ok(jobOfferService.matchesForJob(id));
    }

}
