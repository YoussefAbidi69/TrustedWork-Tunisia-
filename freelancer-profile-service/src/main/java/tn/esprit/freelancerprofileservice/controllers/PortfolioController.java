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
import java.util.stream.Collectors;

/**
 * Controller REST — gestion du portfolio
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final IPortfolioService portfolioService;

    // POST /api/portfolio/user/{userId}
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
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(portfolioService.addPortfolioItem(userId, item)));
    }

    // GET /api/portfolio/user/{userId}
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PortfolioResponse>> getMyPortfolio(@PathVariable Long userId) {
        List<PortfolioResponse> items = portfolioService.getMyPortfolio(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    // PUT /api/portfolio/{itemId}/user/{userId}
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
                .build();

        return ResponseEntity.ok(toResponse(
                portfolioService.updatePortfolioItem(itemId, userId, updates)));
    }

    // DELETE /api/portfolio/{itemId}/user/{userId}
    @DeleteMapping("/{itemId}/user/{userId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long itemId,
            @PathVariable Long userId) {
        portfolioService.deletePortfolioItem(itemId, userId);
        return ResponseEntity.noContent().build();
    }

    private PortfolioResponse toResponse(PortfolioItem i) {
        return PortfolioResponse.builder()
                .id(i.getId())
                .title(i.getTitle())
                .description(i.getDescription())
                .projectUrl(i.getProjectUrl())
                .imageUrl(i.getImageUrl())
                .technologies(i.getTechnologies())
                .completionDate(i.getCompletionDate())
                .build();
    }
}