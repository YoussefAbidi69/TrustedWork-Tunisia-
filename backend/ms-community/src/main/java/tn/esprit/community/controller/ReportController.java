package tn.esprit.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.ReportDTO;
import tn.esprit.community.service.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<ReportDTO> reportPost(
            @RequestParam Long reportedBy,
            @RequestParam Long postId,
            @RequestParam String reason,
            @RequestParam String description) {
        ReportDTO report = reportService.reportPost(reportedBy, postId, reason, description);
        return new ResponseEntity<>(report, HttpStatus.CREATED);
    }

    @PutMapping("/admin/restore")
    public ResponseEntity<ReportDTO> adminRestorePost(@RequestParam Long postId) {
        ReportDTO report = reportService.adminRestorePost(postId);
        return new ResponseEntity<>(report, HttpStatus.OK);
    }

    @PutMapping("/admin/reject")
    public ResponseEntity<ReportDTO> adminRejectPost(@RequestParam Long postId) {
        ReportDTO report = reportService.adminRejectPost(postId);
        return new ResponseEntity<>(report, HttpStatus.OK);
    }
}
