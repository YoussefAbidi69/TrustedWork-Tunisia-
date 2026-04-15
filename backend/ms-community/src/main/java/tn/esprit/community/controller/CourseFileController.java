package tn.esprit.community.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.community.service.impl.CourseFileStorageService;
import tn.esprit.community.service.impl.FilePostUploadService;
import tn.esprit.community.exception.ValidationException;

@RestController
@RequestMapping("/api/course-files")
public class CourseFileController {

    private final CourseFileStorageService storageService;
    private final FilePostUploadService filePostUploadService;

    public CourseFileController(CourseFileStorageService storageService, FilePostUploadService filePostUploadService) {
        this.storageService = storageService;
        this.filePostUploadService = filePostUploadService;
    }

    /**
     * Browsers and tools often issue GET on the base URL; without this handler Spring falls through to
     * {@code ResourceHttpRequestHandler} and responds with "No static resource api/course-files".
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> uploadInstructions() {
        return Map.of(
                "message",
                "POST multipart/form-data part \"file\" (PDF). Uploads are sent to FilePost; JSON body contains fileUrl (CDN). Requires FILEPOST_API_KEY.",
                "method",
                "POST",
                "path",
                "/api/course-files");
    }

    /** No {@code consumes} so multipart requests with boundary always match (avoids mapping misses). */
    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (!filePostUploadService.isEnabled()) {
            throw new ValidationException(
                    "Course PDF upload uses FilePost only. Set environment variable FILEPOST_API_KEY (and optionally FILEPOST_API_BASE_URL).");
        }
        String fileUrl = filePostUploadService.uploadPdf(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("fileUrl", fileUrl));
    }

    @GetMapping("/{storedName:.+}")
    public ResponseEntity<Resource> download(@PathVariable String storedName) throws IOException {
        Path path = storageService.resolveStoredFile(storedName);
        if (path == null || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + storedName + "\"")
                .body(resource);
    }
}
