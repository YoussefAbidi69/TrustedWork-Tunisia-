package tn.esprit.community.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import tn.esprit.community.exception.ValidationException;

/**
 * Proxies course PDF uploads to <a href="https://filepost.dev">FilePost</a> when
 * {@code app.filepost.api-key} is set (use env {@code FILEPOST_API_KEY}).
 */
@Service
public class FilePostUploadService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final CourseFileStorageService courseFileStorageService;
    private final String apiKey;
    private final boolean enabled;

    public FilePostUploadService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            CourseFileStorageService courseFileStorageService,
            @Value("${app.filepost.base-url:https://filepost.dev}") String baseUrl,
            @Value("${app.filepost.api-key:}") String apiKey) {
        this.objectMapper = objectMapper;
        this.courseFileStorageService = courseFileStorageService;
        String trimmed = apiKey == null ? "" : apiKey.trim();
        this.apiKey = trimmed;
        this.enabled = !trimmed.isEmpty();
        String root = baseUrl.replaceAll("/$", "");
        HttpClient reactor = HttpClient.create().responseTimeout(Duration.ofMinutes(2));
        this.webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(reactor))
                .baseUrl(root)
                .build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * POST {@code /v1/upload} with multipart {@code file}; returns public CDN URL for {@link tn.esprit.community.entity.Post#setFileUrl}.
     */
    public String uploadFile(MultipartFile file) throws IOException {
        if (!enabled) {
            throw new IllegalStateException("FilePost is not configured (missing app.filepost.api-key)");
        }
        byte[] content = file.getBytes();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.bin";
        courseFileStorageService.validateMediaContent(content, filename);
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        HttpHeaders partHeaders = new HttpHeaders();
        
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) {
            partHeaders.setContentType(MediaType.IMAGE_PNG);
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            partHeaders.setContentType(MediaType.IMAGE_JPEG);
        } else if (lower.endsWith(".gif")) {
            partHeaders.setContentType(MediaType.IMAGE_GIF);
        } else if (lower.endsWith(".mp4")) {
            partHeaders.setContentType(MediaType.parseMediaType("video/mp4"));
        } else if (lower.endsWith(".webm")) {
            partHeaders.setContentType(MediaType.parseMediaType("video/webm"));
        } else if (lower.endsWith(".pdf")) {
            partHeaders.setContentType(MediaType.APPLICATION_PDF);
        } else {
            partHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }
        
        partHeaders.setContentDispositionFormData("file", filename);
        MultiValueMap<String, HttpEntity<?>> multipart = new LinkedMultiValueMap<>();
        multipart.add("file", new HttpEntity<>(resource, partHeaders));

        String json = webClient
                .post()
                .uri("/v1/upload")
                .header("X-API-Key", apiKey)
                .body(BodyInserters.fromMultipartData(multipart))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class).flatMap(body -> Mono.error(new ValidationException(
                                body != null && body.length() < 800
                                        ? "FilePost: " + body
                                        : "FilePost upload failed (HTTP " + response.statusCode() + ")"))))
                .bodyToMono(String.class)
                .block(Duration.ofMinutes(2));
        if (json == null || json.isBlank()) {
            throw new ValidationException("FilePost returned an empty response");
        }
        JsonNode root = objectMapper.readTree(json);
        String url = extractPublicUrl(root);
        if (url == null || url.isEmpty()) {
            throw new ValidationException("FilePost response had no public URL: " + json);
        }
        return url;
    }

    private static String extractPublicUrl(JsonNode root) {
        String[] keys = {"url", "public_url", "cdn_url", "download_url"};
        for (String k : keys) {
            if (root.hasNonNull(k)) {
                String v = root.get(k).asText().trim();
                if (!v.isEmpty()) {
                    return v;
                }
            }
        }
        return null;
    }
}
