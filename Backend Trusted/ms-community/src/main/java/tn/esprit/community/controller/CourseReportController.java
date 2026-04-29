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
import tn.esprit.community.dto.request.CourseReportRequest;
import tn.esprit.community.dto.response.CourseReportResponse;
import tn.esprit.community.entity.enums.ReportStatus;
import tn.esprit.community.service.CourseReportService;

@RestController
@RequestMapping("/api/course-reports")
public class CourseReportController {
    private final CourseReportService reportService;

    public CourseReportController(CourseReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/course/{courseId}")
    public ResponseEntity<CourseReportResponse> reportCourse(
            @PathVariable Long courseId, @RequestBody CourseReportRequest reportRequest) {
        CourseReportResponse report = reportService.reportCourse(courseId, reportRequest);
        return new ResponseEntity<>(report, HttpStatus.CREATED);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CourseReportResponse>> listReportsByCourse(@PathVariable Long courseId) {
        List<CourseReportResponse> reports = reportService.listReportsByCourse(courseId);
        return new ResponseEntity<>(reports, HttpStatus.OK);
    }

    @PutMapping("/{reportId}/status")
    public ResponseEntity<CourseReportResponse> updateStatus(
            @PathVariable Long reportId, @RequestParam ReportStatus status) {
        CourseReportResponse report = reportService.updateStatus(reportId, status);
        return new ResponseEntity<>(report, HttpStatus.OK);
    }
}
