package tn.esprit.freelancerprofileservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.response.CareerPathResponse;
import tn.esprit.freelancerprofileservice.dto.response.SkillGapResponse;
import tn.esprit.freelancerprofileservice.services.ICareerPathService;
import tn.esprit.freelancerprofileservice.services.ISkillGapService;

/**
 * Controller REST — recommandations IA (Career Path + Skill Gap)
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final ICareerPathService careerPathService;
    private final ISkillGapService skillGapService;

    // GET /api/recommendations/user/{userId}/career-path
    @GetMapping("/user/{userId}/career-path")
    public ResponseEntity<CareerPathResponse> getCareerPath(@PathVariable Long userId) {
        return ResponseEntity.ok(careerPathService.recommendCareerPath(userId));
    }

    // GET /api/recommendations/user/{userId}/skill-gap
    @GetMapping("/user/{userId}/skill-gap")
    public ResponseEntity<SkillGapResponse> getSkillGap(@PathVariable Long userId) {
        return ResponseEntity.ok(skillGapService.detectSkillGaps(userId));
    }
}