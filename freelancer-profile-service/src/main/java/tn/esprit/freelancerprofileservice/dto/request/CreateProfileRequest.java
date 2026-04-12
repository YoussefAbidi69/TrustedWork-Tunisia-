package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import tn.esprit.freelancerprofileservice.enums.AvailabilityStatus;
import tn.esprit.freelancerprofileservice.enums.ProfileVisibility;
import tn.esprit.freelancerprofileservice.enums.ProjectType;

/**
 * DTO de création d'un profil freelancer
 */
@Data
public class CreateProfileRequest {

    @NotNull(message = "L'userId est obligatoire")
    private Long userId;

    private String headline;
    private String bio;
    private String avatarUrl;

    @Positive(message = "Le taux horaire doit être positif")
    private Double hourlyRate;

    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;
    private ProfileVisibility visibility = ProfileVisibility.PUBLIC;
    private ProjectType projectType = ProjectType.BOTH;
    private String region;
}