package tn.esprit.freelancerprofileservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.request.AddReportRequest;
import tn.esprit.freelancerprofileservice.dto.response.ProfileReportResponse;
import tn.esprit.freelancerprofileservice.entities.ProfileReport;
import tn.esprit.freelancerprofileservice.enums.ReportStatus;
import tn.esprit.freelancerprofileservice.services.IProfileReportService;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final IProfileReportService reportService;

    @PostMapping("/profile/{profileId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileReport> reportProfile(
            @PathVariable Long profileId,
            @Valid @RequestBody AddReportRequest request) {

        ProfileReport saved = reportService.reportProfile(
                profileId,
                request.getReporterId(),
                request.getCategory(),
                request.getDescription()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProfileReportResponse>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProfileReportResponse>> getPendingReports() {
        return ResponseEntity.ok(reportService.getPendingReports());
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProfileReportResponse>> getReportsByStatus(@PathVariable ReportStatus status) {
        return ResponseEntity.ok(reportService.getReportsByStatus(status));
    }

    @GetMapping("/profile/{profileId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProfileReportResponse>> getReportsByProfileId(@PathVariable Long profileId) {
        return ResponseEntity.ok(reportService.getReportsByProfileId(profileId));
    }

    @PatchMapping("/{reportId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProfileReportResponse> updateReportStatus(
            @PathVariable Long reportId,
            @RequestParam ReportStatus status) {

        return ResponseEntity.ok(reportService.updateReportStatus(reportId, status));
    }
}