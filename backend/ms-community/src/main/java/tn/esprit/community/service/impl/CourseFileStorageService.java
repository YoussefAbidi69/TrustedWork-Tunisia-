package tn.esprit.community.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.community.exception.ValidationException;

@Service
public class CourseFileStorageService {

    private final Path uploadDirectory;
    /** Public origin for {@code /api/course-files/...} links (see {@code app.external-api-base-url}). */
    private final String externalApiBaseUrl;

    public CourseFileStorageService(
            @Value("${app.course-upload.dir:uploads/courses}") String uploadDir,
            @Value("${app.external-api-base-url:http://localhost:8084}") String externalApiBaseUrl)
            throws IOException {
        this.uploadDirectory = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDirectory);
        this.externalApiBaseUrl = externalApiBaseUrl.replaceAll("/$", "");
    }

    public Path resolveStoredFile(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return null;
        }
        if (storedName.contains("..") || storedName.indexOf('/') >= 0 || storedName.indexOf('\\') >= 0) {
            return null;
        }
        
        String lower = storedName.toLowerCase();
        if (!lower.endsWith(".pdf") && !lower.endsWith(".png") && !lower.endsWith(".jpg") 
            && !lower.endsWith(".jpeg") && !lower.endsWith(".gif") && !lower.endsWith(".mp4") 
            && !lower.endsWith(".webm")) {
            return null;
        }
        
        boolean uuidForm = storedName.matches(
                "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}\\.[a-z0-9]+$");
        boolean legacyBasename = storedName.matches("^[a-zA-Z0-9._-]{1,200}\\.[a-z0-9]+$");
        if (!uuidForm && !legacyBasename) {
            return null;
        }
        Path path = uploadDirectory.resolve(storedName).normalize();
        if (!path.startsWith(uploadDirectory)) {
            return null;
        }
        return path;
    }

    /**
     * Validates media extension and content (shared with remote upload providers).
     */
    public void validateMediaForUpload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is required");
        }
        validateMediaContent(
                file.getBytes(),
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
    }

    public void validateMediaContent(byte[] content, String originalFilename) {
        String original = originalFilename != null ? originalFilename : "";
        if (content == null || content.length == 0) {
            throw new ValidationException("File is required");
        }
        
        String lower = original.toLowerCase();
        if (!lower.endsWith(".pdf") && !lower.endsWith(".png") && !lower.endsWith(".jpg") 
            && !lower.endsWith(".jpeg") && !lower.endsWith(".gif") && !lower.endsWith(".mp4") 
            && !lower.endsWith(".webm")) {
            throw new ValidationException("Only PDF, images, and videos are accepted");
        }
        
        if (lower.endsWith(".pdf")) {
            if (content.length < 4
                    || content[0] != '%'
                    || content[1] != 'P'
                    || content[2] != 'D'
                    || content[3] != 'F') {
                throw new ValidationException("File is not a valid PDF");
            }
        }
    }

    /**
     * Saves a file and returns the absolute HTTP URL to store in {@code Post.fileUrl}.
     */
    public String storeFile(MultipartFile file) throws IOException {
        byte[] content = file.getBytes();
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        validateMediaContent(content, originalName);
        
        String ext = "";
        int i = originalName.lastIndexOf('.');
        if (i > 0) {
            ext = originalName.substring(i);
        } else {
            ext = ".bin";
        }
        
        String stored = UUID.randomUUID() + ext;
        Path target = uploadDirectory.resolve(stored);
        Files.write(target, content);
        return externalApiBaseUrl + "/api/course-files/" + stored;
    }
}
