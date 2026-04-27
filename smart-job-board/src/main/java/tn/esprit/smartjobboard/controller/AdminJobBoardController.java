package tn.esprit.smartjobboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import tn.esprit.smartjobboard.dto.AdminMarketAnalyticsResponse;
import tn.esprit.smartjobboard.dto.JobOfferUpdateRequest;
import tn.esprit.smartjobboard.dto.JobApplicationResponse;
import tn.esprit.smartjobboard.dto.JobOfferResponse;
import tn.esprit.smartjobboard.dto.OfferFlagDto;
import tn.esprit.smartjobboard.dto.PlatformStatsDto;
import tn.esprit.smartjobboard.entity.ApplicationStatus;
import tn.esprit.smartjobboard.service.AdminAnalyticsService;
import tn.esprit.smartjobboard.service.JobApplicationService;
import tn.esprit.smartjobboard.service.JobOfferService;
import tn.esprit.smartjobboard.service.MarketAnalyticsService;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin — Job board")
public class AdminJobBoardController {

    private final JobOfferService jobOfferService;
    private final AdminAnalyticsService adminAnalyticsService;
    private final JobApplicationService jobApplicationService;
    private final MarketAnalyticsService marketAnalyticsService;

    @GetMapping("/jobs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "All job offers")
    public ResponseEntity<Page<JobOfferResponse>> allJobs(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(jobOfferService.adminListAll(pageable, status, category, search));
    }

    @GetMapping("/jobs/flagged")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Flagged job offers")
    public ResponseEntity<Page<JobOfferResponse>> flagged(
            @PageableDefault(size = 50, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(jobOfferService.adminListFlagged(pageable));
    }

    @PutMapping("/jobs/{id}/flag")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually flag a job offer")
    public ResponseEntity<JobOfferResponse> flag(@PathVariable Long id) {
        return ResponseEntity.ok(jobOfferService.adminFlag(id));
    }

    @PutMapping("/jobs/{id}/unflag")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Clear fraud flags and restore a flagged job to published")
    public ResponseEntity<JobOfferResponse> unflag(@PathVariable Long id) {
        return ResponseEntity.ok(jobOfferService.adminUnflag(id));
    }

    @PutMapping("/jobs/{id}/force-close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force-close a job offer (admin)")
    public ResponseEntity<JobOfferResponse> forceClose(@PathVariable Long id) {
        return ResponseEntity.ok(jobOfferService.adminForceClose(id));
    }

    @PatchMapping("/jobs/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobOfferResponse> close(@PathVariable Long id) {
        return ResponseEntity.ok(jobOfferService.adminForceClose(id));
    }

    @PutMapping("/jobs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update any job offer (admin)")
    public ResponseEntity<JobOfferResponse> adminUpdateJob(@PathVariable Long id,
                                                           @Valid @RequestBody JobOfferUpdateRequest body) {
        return ResponseEntity.ok(jobOfferService.adminUpdateJob(id, body));
    }

    @DeleteMapping("/jobs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Permanently delete a job offer and related data (admin)")
    public ResponseEntity<Void> adminDeleteJob(@PathVariable Long id) {
        jobOfferService.adminDeleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/jobs/{id}/flags")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fraud signals recorded for a job")
    public ResponseEntity<List<OfferFlagDto>> jobFlags(@PathVariable Long id) {
        return ResponseEntity.ok(jobOfferService.adminListFlagsForJob(id));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Platform-wide job board statistics")
    public ResponseEntity<PlatformStatsDto> stats() {
        return ResponseEntity.ok(adminAnalyticsService.platformStats());
    }

    @GetMapping("/market-analytics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Full market analytics")
    public ResponseEntity<AdminMarketAnalyticsResponse> marketAnalytics() {
        return ResponseEntity.ok(adminAnalyticsService.fullReport());
    }

    @PostMapping("/market-insights/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Recompute market skill demand (admin path)")
    public ResponseEntity<Void> refreshMarketInsightsAdmin() {
        marketAnalyticsService.aggregateSkillDemand();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "All job applications (paginated)")
    public ResponseEntity<Page<JobApplicationResponse>> applications(
            @PageableDefault(size = 50, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Double minMatchScore
    ) {
        return ResponseEntity.ok(jobApplicationService.listAllForAdmin(pageable, status, minMatchScore));
    }
}
