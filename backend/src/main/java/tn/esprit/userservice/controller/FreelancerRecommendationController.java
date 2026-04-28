package tn.esprit.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.RecommendationFilterDTO;
import tn.esprit.userservice.dto.RecommendationResponseDTO;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.repository.UserRepository;
import tn.esprit.userservice.service.IFreelancerRecommendationService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/agencies")
@RequiredArgsConstructor
@Tag(name = "Agency Recommendation API", description = "ML-Powered Freelancer Recommendation System")
public class FreelancerRecommendationController {

    private final IFreelancerRecommendationService recommendationService;
    private final UserRepository userRepository;

    @Operation(summary = "Get recommended freelancers for an agency")
    @GetMapping("/{agencyId}/recommended-freelancers")
    public ResponseEntity<?> getRecommendedFreelancers(
            @PathVariable Long agencyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Float minScore,
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String availability,
            @RequestParam(defaultValue = "score") String sortBy,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean refresh,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElseThrow();

            RecommendationFilterDTO filters = RecommendationFilterDTO.builder()
                    .minScore(minScore)
                    .skills(skills)
                    .availability(availability)
                    .sortBy(sortBy)
                    .search(search)
                    .refresh(refresh)
                    .build();

            Pageable pageable = PageRequest.of(page, size);
            
            RecommendationResponseDTO response = recommendationService.getRecommendations(agencyId, user.getId(), filters, pageable);
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("data", response);
            return ResponseEntity.ok(body);
        } catch (Throwable e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            
            if (errorMsg.contains("Only a LEAD")) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", errorMsg));
            } else if (errorMsg.contains("Agency not found")) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", errorMsg));
            }
            
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            String fullTrace = errorMsg + "\n" + sw.toString();
            
            return ResponseEntity.status(500).body(Map.of("success", false, "message", fullTrace));
        }
    }
}
