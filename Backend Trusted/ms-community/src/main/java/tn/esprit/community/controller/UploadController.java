package tn.esprit.community.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.community.service.impl.CourseFileStorageService;
import tn.esprit.community.service.impl.FilePostUploadService;

@RestController
@RequestMapping("/api/course-files")
public class UploadController {

    private final CourseFileStorageService courseFileStorageService;
    private final FilePostUploadService filePostUploadService;

    public UploadController(CourseFileStorageService courseFileStorageService, FilePostUploadService filePostUploadService) {
        this.courseFileStorageService = courseFileStorageService;
        this.filePostUploadService = filePostUploadService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String url;
            if (filePostUploadService.isEnabled()) {
                url = filePostUploadService.uploadFile(file);
            } else {
                url = courseFileStorageService.storeFile(file);
            }
            return ResponseEntity.ok(Collections.singletonMap("fileUrl", url));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Could not upload file: " + e.getMessage()));
        }
    }
}
