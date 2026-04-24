package tn.esprit.freelancerprofileservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.request.AddPortfolioRequest;
import tn.esprit.freelancerprofileservice.dto.response.PortfolioResponse;
import tn.esprit.freelancerprofileservice.entities.PortfolioItem;
import tn.esprit.freelancerprofileservice.services.IPortfolioService;

import java.util.List;

/**
 * Controller REST — gestion du portfolio
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final IPortfolioService portfolioService;

    @PostMapping("/user/{userId}")
    public ResponseEntity<PortfolioResponse> addItem(
            @PathVariable Long userId,
            @Valid @RequestBody AddPortfolioRequest request) {

        PortfolioItem item = PortfolioItem.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .projectUrl(request.getProjectUrl())
                .imageUrl(request.getImageUrl())
                .technologies(request.getTechnologies())
                .completionDate(request.getCompletionDate())
                .pinned(Boolean.TRUE.equals(request.getPinned()))
                .build();

        PortfolioItem savedItem = portfolioService.addPortfolioItem(userId, item);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(savedItem));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PortfolioResponse>> getMyPortfolio(@PathVariable Long userId) {
        List<PortfolioResponse> items = portfolioService.getMyPortfolio(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(items);
    }

    @GetMapping("/user/{userId}/pinned")
    public ResponseEntity<List<PortfolioResponse>> getPinnedPortfolio(@PathVariable Long userId) {
        List<PortfolioResponse> items = portfolioService.getPinnedPortfolio(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(items);
    }

    @PutMapping("/{itemId}/user/{userId}")
    public ResponseEntity<PortfolioResponse> updateItem(
            @PathVariable Long itemId,
            @PathVariable Long userId,
            @Valid @RequestBody AddPortfolioRequest request) {

        PortfolioItem updates = PortfolioItem.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .projectUrl(request.getProjectUrl())
                .imageUrl(request.getImageUrl())
                .technologies(request.getTechnologies())
                .completionDate(request.getCompletionDate())
                .pinned(Boolean.TRUE.equals(request.getPinned()))
                .build();

        PortfolioItem updatedItem = portfolioService.updatePortfolioItem(itemId, userId, updates);

        return ResponseEntity.ok(toResponse(updatedItem));
    }

    @PatchMapping("/{itemId}/user/{userId}/pin")
    public ResponseEntity<PortfolioResponse> pinItem(
            @PathVariable Long itemId,
            @PathVariable Long userId) {

        PortfolioItem pinnedItem = portfolioService.pinPortfolioItem(itemId, userId);
        return ResponseEntity.ok(toResponse(pinnedItem));
    }

    @PatchMapping("/{itemId}/user/{userId}/unpin")
    public ResponseEntity<PortfolioResponse> unpinItem(
            @PathVariable Long itemId,
            @PathVariable Long userId) {

        PortfolioItem unpinnedItem = portfolioService.unpinPortfolioItem(itemId, userId);
        return ResponseEntity.ok(toResponse(unpinnedItem));
    }

    @DeleteMapping("/{itemId}/user/{userId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long itemId,
            @PathVariable Long userId) {

        portfolioService.deletePortfolioItem(itemId, userId);
        return ResponseEntity.noContent().build();
    }

    private PortfolioResponse toResponse(PortfolioItem item) {
        return PortfolioResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .projectUrl(item.getProjectUrl())
                .imageUrl(item.getImageUrl())
                .technologies(item.getTechnologies())
                .completionDate(item.getCompletionDate())
                .pinned(Boolean.TRUE.equals(item.getPinned()))
                .projectScore(calculateProjectScore(item))
                .build();
    }

    private int calculateProjectScore(PortfolioItem item) {
        int score = 0;

        if (item.getTitle() != null && !item.getTitle().isBlank()) {
            score += 20;
        }
        if (item.getDescription() != null && !item.getDescription().isBlank()) {
            score += 20;
        }
        if (item.getTechnologies() != null && !item.getTechnologies().isBlank()) {
            score += 20;
        }
        if (item.getProjectUrl() != null && !item.getProjectUrl().isBlank()) {
            score += 20;
        }
        if (item.getCompletionDate() != null) {
            score += 20;
        }

        return score;
    }
}