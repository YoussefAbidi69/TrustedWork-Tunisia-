package tn.esprit.smartjobboard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PreviewSkillsRequest {
    @NotBlank
    private String description;
}
