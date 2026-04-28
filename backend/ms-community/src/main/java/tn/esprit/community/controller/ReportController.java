package tn.esprit.community.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.request.ReportRequest;
import tn.esprit.community.dto.response.ReportResponse;
import tn.esprit.community.entity.enums.ReportStatus;
import tn.esprit.community.service.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/post/{postId}")
    public ResponseEntity<ReportResponse> reportPost(
            @PathVariable Long postId, @RequestBody ReportRequest reportRequest) {
        ReportResponse report = reportService.reportPost(postId, reportRequest);
        return new ResponseEntity<>(report, HttpStatus.CREATED);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<ReportResponse>> listReportsByPost(@PathVariable Long postId) {
        List<ReportResponse> reports = reportService.listReportsByPost(postId);
        return new ResponseEntity<>(reports, HttpStatus.OK);
    }

    @PostMapping("/course/{courseId}")
    public ResponseEntity<ReportResponse> reportCourse(
            @PathVariable Long courseId, @RequestBody ReportRequest reportRequest) {
        ReportResponse report = reportService.reportCourse(courseId, reportRequest);
        return new ResponseEntity<>(report, HttpStatus.CREATED);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ReportResponse>> listReportsByCourse(@PathVariable Long courseId) {
        List<ReportResponse> reports = reportService.listReportsByCourse(courseId);
        return new ResponseEntity<>(reports, HttpStatus.OK);
    }

    @PutMapping("/{reportId}/status")
    public ResponseEntity<ReportResponse> updateStatus(
            @PathVariable Long reportId, @RequestParam ReportStatus status) {
        ReportResponse report = reportService.updateStatus(reportId, status);
        return new ResponseEntity<>(report, HttpStatus.OK);
    }
}
