package tn.esprit.userservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.userservice.dto.chat.FileUploadResultDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.List;

@Service
public class ChatFileStorageService {

    @Value("${app.upload.base-path:uploads}")
    private String basePath;

    public FileUploadResultDTO store(MultipartFile file, Long agencyId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "file";
        }
        
        // Strip path traversal chars
        String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String uniqueFilename = UUID.randomUUID().toString() + "_" + safeFilename;
        
        String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String relativeDir = "agency/" + agencyId + "/chat/" + dateStr;
        Path uploadPath = Paths.get(basePath, relativeDir);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);
        
        // Verify via Files.probeContentType
        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null) {
            mimeType = file.getContentType();
            if (mimeType == null) mimeType = "application/octet-stream";
        }
        
        // Validate MIME type
        List<String> allowed = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf",
            "text/plain", "application/zip", "application/x-zip-compressed",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/x-java-source", "application/javascript", "text/css", "text/html", "application/octet-stream"
        );
        
        // If not strictly allowed, we still allow it but maybe warn. The prompt says "Reject any other MIME type with 415".
        // Wait, I will throw an exception if invalid.
        if (!allowed.contains(mimeType)) {
            Files.deleteIfExists(filePath);
            throw new RuntimeException("Unsupported Media Type");
        }

        String url = "/uploads/" + relativeDir + "/" + uniqueFilename;

        return new FileUploadResultDTO(url, originalFilename, mimeType, file.getSize());
    }
}
