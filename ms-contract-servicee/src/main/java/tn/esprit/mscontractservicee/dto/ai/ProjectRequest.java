package tn.esprit.mscontractservicee.dto.ai;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.io.Serializable;

@Data
public class ProjectRequest implements Serializable {

    @NotBlank(message = "La description est requise")
    private String description;

    @NotNull(message = "Le budget est requis")
    @Positive(message = "Le budget doit être positif")
    private Double budget;

    @NotNull(message = "Le délai est requis")
    @Min(value = 1, message = "Le délai minimum est 1 jour")
    private Integer deadlineDays;  // ← Utiliser Integer, pas LocalDate

    @NotBlank(message = "La catégorie est requise")
    private String category;

    @NotBlank(message = "Le mode d'optimisation est requis")
    private String optimizationMode;
}