package tn.esprit.freelancerprofileservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.request.AddReviewRequest;
import tn.esprit.freelancerprofileservice.dto.response.ReviewResponse;
import tn.esprit.freelancerprofileservice.entities.ProfileReview;
import tn.esprit.freelancerprofileservice.services.IProfileReviewService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST — gestion des avis clients
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final IProfileReviewService reviewService;

    // POST /api/reviews/profile/{profileId}
    @PostMapping("/profile/{profileId}")
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable Long profileId,
            @Valid @RequestBody AddReviewRequest request) {

        ProfileReview saved = reviewService.addReview(
                profileId, request.getClientId(),
                request.getRating(), request.getComment());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    // GET /api/reviews/profile/{profileId}
    @GetMapping("/profile/{profileId}")
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Long profileId) {
        List<ReviewResponse> reviews = reviewService.getVisibleReviews(profileId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(reviews);
    }

    // GET /api/reviews/profile/{profileId}/average
    @GetMapping("/profile/{profileId}/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long profileId) {
        return ResponseEntity.ok(reviewService.getAverageRating(profileId));
    }

    private ReviewResponse toResponse(ProfileReview r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .clientId(r.getClientId())
                .rating(r.getRating())
                .comment(r.getComment())
                .status(r.getStatus())
                .reviewedAt(r.getReviewedAt())
                .build();
    }
}