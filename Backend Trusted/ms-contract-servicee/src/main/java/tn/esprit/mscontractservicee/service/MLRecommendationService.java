package tn.esprit.mscontractservicee.service;

import tn.esprit.mscontractservicee.dto.ai.ProjectRequest;
import tn.esprit.mscontractservicee.dto.ai.MlProjectRequest;
import tn.esprit.mscontractservicee.dto.ai.MlRecommendationResponse;
import tn.esprit.mscontractservicee.dto.ai.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MLRecommendationService {

    private final WebClient mlWebClient;

    @Value("${ml.service.timeout:30000}")
    private int timeout;

    /**
     * Obtenir des recommandations de freelancers pour un projet
     */
    public Mono<List<RecommendationResponse>> getRecommendations(ProjectRequest request) {
        log.info("📡 Appel du service ML pour projet: {}", request.getCategory());
        log.debug("   Budget: {} USD, Délai: {} jours", request.getBudget(), request.getDeadlineDays());

        MlProjectRequest mlReq = MlProjectRequest.builder()
                .description(request.getDescription())
                .budget(request.getBudget() != null ? request.getBudget() : 0.0d)
                .deadlineDays(request.getDeadlineDays() != null ? request.getDeadlineDays() : 1)
                .category(request.getCategory())
                .optimizationMode(normalizeOptimizationMode(request.getOptimizationMode()))
                .build();

        return mlWebClient.post()
                .uri("/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mlReq)
                .retrieve()
                .bodyToFlux(MlRecommendationResponse.class)
                .map(MlRecommendationResponse::toPublicDto)
                .collectList()
                .doOnError(WebClientResponseException.class, ex ->
                        log.error("ML service error: HTTP {} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString()))
                .doOnSuccess(result -> log.info("✅ {} freelancers recommandés", result.size()))
                .doOnError(error -> log.error("❌ Erreur ML: {}", error.getMessage()));
    }

    /**
     * Vérifier la santé du service ML
     */
    public Mono<Boolean> isMLServiceAvailable() {
        return mlWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> response.contains("ok"))
                .onErrorReturn(false);
    }

    private static String normalizeOptimizationMode(String raw) {
        if (raw == null) return "best";
        String v = raw.trim().toLowerCase();
        return switch (v) {
            case "best", "best_overall", "overall" -> "best";
            case "fastest", "fast" -> "fastest";
            case "best_value", "bestvalue", "value" -> "best_value";
            case "lowest_risk", "low_risk", "risk" -> "lowest_risk";
            default -> v;
        };
    }
}
