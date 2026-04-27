package tn.esprit.smartjobboard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SuccessPredictionPostRequest {
    @NotNull
    private Long jobOfferId;
    @NotNull
    private Long freelancerId;
    /** Optional; when present, may be used by future scoring refinements. */
    private List<String> freelancerSkills;
}
