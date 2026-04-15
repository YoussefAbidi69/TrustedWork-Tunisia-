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
        if (!storedName.toLowerCase().endsWith(".pdf")) {
            return null;
        }
        boolean uuidForm = storedName.matches(
                "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}\\.pdf$");
        boolean legacyBasename = storedName.matches("^[a-zA-Z0-9._-]{1,200}\\.pdf$");
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
     * Validates PDF extension and magic bytes (shared with remote upload providers).
     */
    public void validatePdfForUpload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is required");
        }
        validatePdfContent(
                file.getBytes(),
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
    }

    public void validatePdfContent(byte[] content, String originalFilename) {
        String original = originalFilename != null ? originalFilename : "";
        if (content == null || content.length == 0) {
            throw new ValidationException("File is required");
        }
        if (!original.toLowerCase().endsWith(".pdf")) {
            throw new ValidationException("Only PDF files are accepted");
        }
        if (content.length < 4
                || content[0] != '%'
                || content[1] != 'P'
                || content[2] != 'D'
                || content[3] != 'F') {
            throw new ValidationException("File is not a valid PDF");
        }
    }

    /**
     * Saves a PDF and returns the absolute HTTP URL to store in {@code Post.fileUrl}.
     */
    public String storePdf(MultipartFile file) throws IOException {
        byte[] content = file.getBytes();
        validatePdfContent(
                content,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        String stored = UUID.randomUUID() + ".pdf";
        Path target = uploadDirectory.resolve(stored);
        Files.write(target, content);
        return externalApiBaseUrl + "/api/course-files/" + stored;
    }
}
