package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;
import tn.esprit.freelancerprofileservice.enums.AvailabilityStatus;
import tn.esprit.freelancerprofileservice.enums.ProfileVisibility;
import tn.esprit.freelancerprofileservice.enums.ProjectType;

import java.time.LocalDateTime;

/**
 * DTO de réponse — profil freelancer (sans données sensibles)
 */
@Data
@Builder
public class ProfileResponse {

    private Long id;
    private Long userId;
    private String headline;
    private String bio;
    private String avatarUrl;
    private Double hourlyRate;
    private AvailabilityStatus availabilityStatus;
    private ProfileVisibility visibility;
    private ProjectType projectType;
    private Integer completenessScore;
    private String region;
    private Integer regionalRank;
    private Integer totalViews;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}