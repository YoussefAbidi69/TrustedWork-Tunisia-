package tn.esprit.community.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.response.CourseDownloadResponse;
import tn.esprit.community.service.DownloadService;

@RestController
@RequestMapping("/api/download")
public class DownloadController {
    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDownloadResponse> download(@PathVariable Long courseId) {
        return ResponseEntity.ok(downloadService.downloadCourse(courseId));
    }
}
