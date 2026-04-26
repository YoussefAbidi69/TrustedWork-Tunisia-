package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import tn.esprit.freelancerprofileservice.enums.AvailabilityStatus;
import tn.esprit.freelancerprofileservice.enums.ProfileVisibility;
import tn.esprit.freelancerprofileservice.enums.ProjectType;

/**
 * DTO de mise à jour d'un profil freelancer
 */
@Data
public class UpdateProfileRequest {

    private String headline;
    private String bio;
    private String avatarUrl;

    @Positive(message = "Le taux horaire doit être positif")
    private Double hourlyRate;

    private AvailabilityStatus availabilityStatus;
    private ProfileVisibility visibility;
    private ProjectType projectType;
    private String region;
}