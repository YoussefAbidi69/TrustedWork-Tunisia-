package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.AgencyReviewRequestDto;
import tn.esprit.userservice.dto.AgencyReviewResponseDto;
import tn.esprit.userservice.entity.AgencyReview;
import tn.esprit.userservice.entity.ReviewTargetType;
import tn.esprit.userservice.mapper.AgencyReviewMapper;
import tn.esprit.userservice.service.IAgencyReviewServices;

import java.util.List;

@RestController
@RequestMapping("/agency-reviews")
@RequiredArgsConstructor
public class AgencyReviewController {

    private final IAgencyReviewServices agencyReviewService;
    private final AgencyReviewMapper agencyReviewMapper;

    // CREATE REVIEW
    @PostMapping("/agency/{agencyId}")
    public AgencyReviewResponseDto createReview(
            @PathVariable Long agencyId,
            @RequestBody AgencyReviewRequestDto dto
    ) {
        AgencyReview review = agencyReviewMapper.toEntity(dto);
        AgencyReview savedReview = agencyReviewService.createReview(agencyId, review);
        return agencyReviewMapper.toResponseDto(savedReview);
    }

    // GET ALL REVIEWS OF AN AGENCY
    @GetMapping("/agency/{agencyId}")
    public List<AgencyReviewResponseDto> getReviewsByAgency(@PathVariable Long agencyId) {
        return agencyReviewService.getReviewsByAgency(agencyId)
                .stream()
                .map(agencyReviewMapper::toResponseDto)
                .toList();
    }

    // GET REVIEWS BY TARGET TYPE
    @GetMapping("/agency/{agencyId}/type")
    public List<AgencyReviewResponseDto> getReviewsByAgencyAndTargetType(
            @PathVariable Long agencyId,
            @RequestParam ReviewTargetType targetType
    ) {
        return agencyReviewService.getReviewsByAgencyAndTargetType(agencyId, targetType)
                .stream()
                .map(agencyReviewMapper::toResponseDto)
                .toList();
    }

    // GET REVIEWS BY PROJECT
    @GetMapping("/project/{projectId}")
    public List<AgencyReviewResponseDto> getReviewsByProject(@PathVariable Long projectId) {
        return agencyReviewService.getReviewsByProject(projectId)
                .stream()
                .map(agencyReviewMapper::toResponseDto)
                .toList();
    }

    // GET AVERAGE RATING OF AN AGENCY
    @GetMapping("/agency/{agencyId}/average-rating")
    public double getAverageRatingByAgency(@PathVariable Long agencyId) {
        return agencyReviewService.getAverageRatingByAgency(agencyId);
    }

    // GET REVIEW BY ID
    @GetMapping("/{reviewId}")
    public AgencyReviewResponseDto getReviewById(@PathVariable Long reviewId) {
        AgencyReview review = agencyReviewService.getReviewById(reviewId);
        return agencyReviewMapper.toResponseDto(review);
    }

    // DELETE REVIEW
    @DeleteMapping("/{reviewId}")
    public void deleteReview(@PathVariable Long reviewId) {
        agencyReviewService.deleteReview(reviewId);
    }
}