package tn.esprit.freelancerprofileservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.request.AddReviewRequest;
import tn.esprit.freelancerprofileservice.dto.request.ReplyToReviewRequest;
import tn.esprit.freelancerprofileservice.dto.response.ProfileReviewSummaryResponse;
import tn.esprit.freelancerprofileservice.dto.response.ReviewResponse;
import tn.esprit.freelancerprofileservice.services.IProfileReviewService;

import java.util.List;

/**
 * Controller REST — gestion des avis clients
 *
 * Endpoints :
 * - Ajouter un avis
 * - Récupérer les avis d'un profil
 * - Obtenir la moyenne
 * - Obtenir le summary (moyenne + distribution)
 * - Répondre à un avis
 * - Masquer un avis (admin)
 * - Supprimer un avis (admin)
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final IProfileReviewService reviewService;

    /**
     * POST /api/reviews/profiles/{profileId}
     */
    @PostMapping("/profiles/{profileId}")
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable Long profileId,
            @Valid @RequestBody AddReviewRequest request) {

        ReviewResponse response = reviewService.addReview(profileId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/reviews/profiles/{profileId}
     * Avis publics uniquement (VISIBLE) — page publique
     */
    @GetMapping("/profiles/{profileId}")
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Long profileId) {

        List<ReviewResponse> reviews = reviewService.getVisibleReviews(profileId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * GET /api/reviews/profiles/{profileId}/all
     * Tous les avis (VISIBLE + HIDDEN + FLAGGED) — dashboard propriétaire uniquement
     */
    @GetMapping("/profiles/{profileId}/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'FREELANCER')")
    public ResponseEntity<List<ReviewResponse>> getAllReviews(@PathVariable Long profileId) {

        List<ReviewResponse> reviews = reviewService.getAllReviews(profileId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * GET /api/reviews/profiles/{profileId}/average
     */
    @GetMapping("/profiles/{profileId}/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long profileId) {

        return ResponseEntity.ok(reviewService.getAverageRating(profileId));
    }

    /**
     * GET /api/reviews/profiles/{profileId}/summary
     */
    @GetMapping("/profiles/{profileId}/summary")
    public ResponseEntity<ProfileReviewSummaryResponse> getSummary(@PathVariable Long profileId) {

        return ResponseEntity.ok(reviewService.getReviewSummary(profileId));
    }

    /**
     * PUT /api/reviews/{reviewId}/reply
     *
     * ⚠️ Pour l’instant on passe freelancerUserId en paramètre.
     * Plus tard → récupérer depuis JWT (SecurityContext)
     */
    @PutMapping("/{reviewId}/reply")
    public ResponseEntity<ReviewResponse> replyToReview(
            @PathVariable Long reviewId,
            @RequestParam Long freelancerUserId,
            @Valid @RequestBody ReplyToReviewRequest request) {

        ReviewResponse response = reviewService.replyToReview(
                reviewId,
                freelancerUserId,
                request
        );

        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/reviews/{reviewId}/hide
     * Le propriétaire (FREELANCER) peut masquer ses propres avis ; l'admin peut tout masquer.
     */
    @PatchMapping("/{reviewId}/hide")
    @PreAuthorize("hasAnyRole('ADMIN', 'FREELANCER')")
    public ResponseEntity<Void> hideReview(@PathVariable Long reviewId) {
        reviewService.hideReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/reviews/{reviewId}
     */
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'FREELANCER')")
    public ResponseEntity<ReviewResponse> restoreReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.restoreReview(id));
    }
}