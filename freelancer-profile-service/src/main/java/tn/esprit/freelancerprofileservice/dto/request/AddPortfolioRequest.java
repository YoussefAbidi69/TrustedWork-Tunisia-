package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO d'ajout d'un projet portfolio
 */
@Data
public class AddPortfolioRequest {

    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    private String description;
    private String projectUrl;
    private String imageUrl;
    private String technologies;
    private LocalDate completionDate;
}