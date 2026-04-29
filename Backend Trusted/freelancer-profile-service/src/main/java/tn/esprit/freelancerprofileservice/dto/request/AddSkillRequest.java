package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tn.esprit.freelancerprofileservice.enums.SkillCategory;

/**
 * DTO d'ajout d'une compétence.
 */
@Data
public class AddSkillRequest {

    @NotBlank(message = "Le nom du skill est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom du skill doit contenir entre 2 et 50 caractères")
    private String name;

    @NotNull(message = "La catégorie du skill est obligatoire")
    private SkillCategory category;

    @Min(value = 0, message = "Le score d'examen ne peut pas être négatif")
    @Max(value = 100, message = "Le score d'examen ne peut pas dépasser 100")
    private Double examScore = 0.0;
}