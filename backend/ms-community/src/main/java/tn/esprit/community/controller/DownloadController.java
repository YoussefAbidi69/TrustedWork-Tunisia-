package tn.esprit.community.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tn.esprit.community.service.DownloadService;

@RestController
@RequestMapping("/api/download")
public class DownloadController {
    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @GetMapping("/{postId}")
    public ResponseEntity<StreamingResponseBody> download(
            @PathVariable Long postId, @RequestParam Long userId) {
        return downloadService.downloadCourse(userId, postId);
    }
}
