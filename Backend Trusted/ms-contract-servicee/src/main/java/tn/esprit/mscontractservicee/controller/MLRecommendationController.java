package tn.esprit.mscontractservicee.controller;

import tn.esprit.mscontractservicee.dto.ai.ProjectRequest;
import tn.esprit.mscontractservicee.dto.ai.RecommendationResponse;
import tn.esprit.mscontractservicee.service.MLRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/recommendations", "/api/v1/recommendations"})
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MLRecommendationController {

    private final MLRecommendationService recommendationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(
            @Valid @RequestBody ProjectRequest request) {

        log.info("📝 Nouvelle demande de recommandation");
        log.info("   Description: {}", request.getDescription().substring(0, Math.min(50, request.getDescription().length())) + "...");
        log.info("   Catégorie: {}", request.getCategory());
        log.info("   Budget: {} USD", request.getBudget());
        log.info("   Mode: {}", request.getOptimizationMode());

        List<RecommendationResponse> recommendations = recommendationService
                .getRecommendations(request)
                .block(); // Bloquant pour la simplicité, utiliser async en prod

        if (recommendations == null || recommendations.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/health")
    public ResponseEntity<Boolean> healthCheck() {
        Boolean isAvailable = recommendationService.isMLServiceAvailable().block();
        return ResponseEntity.ok(isAvailable);
    }
}
