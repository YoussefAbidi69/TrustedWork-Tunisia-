package tn.esprit.community.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/quality")
public class CourseQualityController {

    private static final String KEY_AVAILABLE = "available";
    private static final String KEY_IS_PLAGIARIZED = "is_plagiarized";
    private static final String PLAGIARISM_UNAVAILABLE = "Plagiarism service unavailable";

    private final WebClient qualityClient;

    public CourseQualityController(
            @Value("${app.course-quality.base-url:http://localhost:5000}") String qualityBaseUrl) {
        this.qualityClient = WebClient.builder().baseUrl(qualityBaseUrl).build();
    }

    @PostMapping("/predict")
    public ResponseEntity<Map<String, Object>> predict(@RequestBody Map<String, Object> request) {
        String title = String.valueOf(request.getOrDefault("title", "")).trim();
        String description = String.valueOf(request.getOrDefault("description", "")).trim();

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("description", description);
        payload.put("comment_count", 0);
        payload.put("report_count", 0);

        try {
            Map<String, Object> response = qualityClient.post()
                    .uri("/predict")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return ResponseEntity.ok(response == null ? Map.of(KEY_AVAILABLE, false) : response);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(KEY_AVAILABLE, false));
        }
    }

    @PostMapping("/check-plagiarism")
    public ResponseEntity<Map<String, Object>> checkPlagiarism(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> response = qualityClient.post()
                    .uri("/check_plagiarism")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return ResponseEntity.ok(response == null ? Map.of(KEY_IS_PLAGIARIZED, false) : response);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(KEY_IS_PLAGIARIZED, false, "error", PLAGIARISM_UNAVAILABLE));
        }
    }
}
