package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO d'ajout d'une expérience professionnelle
 */
@Data
public class AddWorkExperienceRequest {

    @NotBlank(message = "Le titre du poste est obligatoire")
    private String jobTitle;

    @NotBlank(message = "L'entreprise est obligatoire")
    private String company;

    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent = false;
}