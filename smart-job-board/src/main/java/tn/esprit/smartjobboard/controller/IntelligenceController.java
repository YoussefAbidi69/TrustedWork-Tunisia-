package tn.esprit.smartjobboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.smartjobboard.dto.CareerInsightResponse;
import tn.esprit.smartjobboard.dto.GenerateCoverLetterRequest;
import tn.esprit.smartjobboard.dto.GenerateCoverLetterResponse;
import tn.esprit.smartjobboard.dto.JobRecommendationRowDto;
import tn.esprit.smartjobboard.dto.MarketSkillInsightDto;
import tn.esprit.smartjobboard.dto.SuccessPredictionPostRequest;
import tn.esprit.smartjobboard.dto.SuccessPredictionViewDto;
import tn.esprit.smartjobboard.service.AiCareerRoadmapService;
import tn.esprit.smartjobboard.service.AiWritingService;
import tn.esprit.smartjobboard.service.CareerTrajectoryService;
import tn.esprit.smartjobboard.service.CurrentUserService;
import tn.esprit.smartjobboard.service.JobOfferService;
import tn.esprit.smartjobboard.service.MarketAnalyticsService;
import tn.esprit.smartjobboard.service.RecommendationService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Intelligence")
public class IntelligenceController {

    private final RecommendationService recommendationService;
    private final MarketAnalyticsService marketAnalyticsService;
    private final CareerTrajectoryService careerTrajectoryService;
    private final JobOfferService jobOfferService;
    private final CurrentUserService currentUserService;
    private final AiWritingService aiWritingService;
    private final AiCareerRoadmapService aiCareerRoadmapService;

    @PostMapping("/generate-cover-letter")
    @Operation(summary = "Generate a personalized cover letter using the Antigravity AI Writing Engine")
    public ResponseEntity<GenerateCoverLetterResponse> generateCoverLetter(@RequestBody GenerateCoverLetterRequest request) {
        String draft = aiWritingService.generateCoverLetter(request);
        return ResponseEntity.ok(new GenerateCoverLetterResponse(draft));
    }

    @GetMapping("/recommendations/{freelancerId}")
    @PreAuthorize("hasRole('FREELANCER')")
    @Operation(summary = "Top 10 job recommendations for a freelancer")
    public ResponseEntity<List<JobRecommendationRowDto>> recommendations(
            @PathVariable Long freelancerId,
            @RequestParam(required = false) List<String> skills
    ) {
        return ResponseEntity.ok(recommendationService.recommend(freelancerId, skills));
    }

    @GetMapping("/recommendations")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<List<JobRecommendationRowDto>> myRecommendations(
            @RequestParam(required = false) List<String> skills
    ) {
        Long currentUserId = currentUserService.requireCurrentUser().getId();
        return ResponseEntity.ok(recommendationService.recommend(currentUserId, skills));
    }

    @PostMapping("/success-prediction")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Success prediction for a freelancer on a job (same rules as GET /jobs/{id}/success-prediction)")
    public ResponseEntity<SuccessPredictionViewDto> successPredictionPost(
            @Valid @RequestBody SuccessPredictionPostRequest body
    ) {
        return ResponseEntity.ok(jobOfferService.getSuccessPrediction(body.getJobOfferId(), body.getFreelancerId()));
    }

    @GetMapping("/market-insights")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Skill demand trends from published jobs")
    public ResponseEntity<List<MarketSkillInsightDto>> marketInsights() {
        return ResponseEntity.ok(marketAnalyticsService.computeMarketInsights());
    }

    @GetMapping("/market/skills")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MarketSkillInsightDto>> marketSkills() {
        return ResponseEntity.ok(marketAnalyticsService.computeMarketInsights());
    }

    @PostMapping("/market-insights/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually refresh market skill demand aggregation (returns latest snapshot)")
    public ResponseEntity<List<MarketSkillInsightDto>> refreshMarketInsights() {
        return ResponseEntity.ok(marketAnalyticsService.computeMarketInsights());
    }

    @GetMapping("/career-insights/{freelancerId}")
    @PreAuthorize("hasRole('FREELANCER')")
    @Operation(summary = "Career trajectory suggestions powered by AI")
    public ResponseEntity<CareerInsightResponse> career(
            @PathVariable Long freelancerId,
            @RequestParam(required = false) List<String> skills
    ) {
        return ResponseEntity.ok(aiCareerRoadmapService.generateRoadmap(skills));
    }

    @PostMapping("/career/trajectory")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<CareerInsightResponse> careerTrajectory(
            @RequestBody(required = false) Map<String, List<String>> body
    ) {
        Long currentUserId = currentUserService.requireCurrentUser().getId();
        List<String> skills = body != null ? body.get("skills") : null;
        return ResponseEntity.ok(careerTrajectoryService.insights(currentUserId, skills));
    }
}
