package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO d'ajout / modification d'une expérience professionnelle
 */
@Data
public class AddWorkExperienceRequest {

    @NotBlank(message = "Le titre du poste est obligatoire")
    @Size(max = 150, message = "Le titre ne doit pas dépasser 150 caractères")
    private String jobTitle;

    @NotBlank(message = "L'entreprise est obligatoire")
    @Size(max = 150, message = "Le nom de l'entreprise ne doit pas dépasser 150 caractères")
    private String company;


    @Size(max = 150, message = "La localisation ne doit pas dépasser 150 caractères")
    private String location;


    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
    private String description;


    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;


    private LocalDate endDate;


    private Boolean isCurrent = false;
}