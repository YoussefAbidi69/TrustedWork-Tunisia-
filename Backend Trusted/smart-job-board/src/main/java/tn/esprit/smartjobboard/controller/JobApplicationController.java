package tn.esprit.smartjobboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.smartjobboard.dto.ApplicationCreateRequest;
import tn.esprit.smartjobboard.dto.ApplicationStatusUpdateRequest;
import tn.esprit.smartjobboard.dto.JobApplicationResponse;
import tn.esprit.smartjobboard.entity.ApplicationStatus;
import tn.esprit.smartjobboard.service.JobApplicationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @PostMapping("/applications")
    @PreAuthorize("hasRole('FREELANCER')")
    @Operation(summary = "Submit application")
    public ResponseEntity<JobApplicationResponse> submit(@Valid @RequestBody ApplicationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobApplicationService.submit(request));
    }

    @GetMapping("/applications/my")
    @PreAuthorize("hasRole('FREELANCER')")
    @Operation(summary = "List authenticated freelancer's applications with AI scores")
    public ResponseEntity<List<JobApplicationResponse>> myApplications() {
        return ResponseEntity.ok(jobApplicationService.listMineForFreelancer());
    }

    @PutMapping("/applications/{id}/status")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Update application status (job owner)")
    public ResponseEntity<JobApplicationResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationStatusUpdateRequest body
    ) {
        return ResponseEntity.ok(jobApplicationService.updateStatus(id, body));
    }

    @PostMapping("/applications/{id}/withdraw")
    @PreAuthorize("hasRole('FREELANCER')")
    @Operation(summary = "Withdraw own application")
    public ResponseEntity<JobApplicationResponse> withdraw(@PathVariable Long id) {
        return ResponseEntity.ok(jobApplicationService.withdraw(id));
    }

    @PatchMapping("/applications/{id}/withdraw")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<JobApplicationResponse> withdrawPatch(@PathVariable Long id) {
        return ResponseEntity.ok(jobApplicationService.withdraw(id));
    }

    @PatchMapping("/applications/{id}/shortlist")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<JobApplicationResponse> shortlist(@PathVariable Long id) {
        ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();
        req.setStatus(ApplicationStatus.SHORTLISTED);
        return ResponseEntity.ok(jobApplicationService.updateStatus(id, req));
    }

    @PatchMapping("/applications/{id}/accept")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<JobApplicationResponse> accept(@PathVariable Long id) {
        ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();
        req.setStatus(ApplicationStatus.ACCEPTED);
        return ResponseEntity.ok(jobApplicationService.updateStatus(id, req));
    }

    @PatchMapping("/applications/{id}/reject")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<JobApplicationResponse> reject(@PathVariable Long id) {
        ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();
        req.setStatus(ApplicationStatus.REJECTED);
        return ResponseEntity.ok(jobApplicationService.updateStatus(id, req));
    }
}
