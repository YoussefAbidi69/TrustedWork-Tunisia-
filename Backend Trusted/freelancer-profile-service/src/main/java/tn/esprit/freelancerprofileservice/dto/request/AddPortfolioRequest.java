package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

/**
 * DTO d'ajout d'un projet portfolio
 */
@Data
public class AddPortfolioRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 150, message = "Le titre ne doit pas dépasser 150 caractères")
    private String title;

    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
    private String description;

    @URL(message = "L'URL du projet est invalide")
    @Size(max = 500, message = "L'URL du projet est trop longue")
    private String projectUrl;

    @URL(message = "L'URL de l'image est invalide")
    @Size(max = 500, message = "L'URL de l'image est trop longue")
    private String imageUrl;

    @Size(max = 500, message = "Les technologies sont trop longues")
    private String technologies;

    @PastOrPresent(message = "La date de réalisation ne peut pas être dans le futur")
    private LocalDate completionDate;

    private Boolean pinned = false;
}