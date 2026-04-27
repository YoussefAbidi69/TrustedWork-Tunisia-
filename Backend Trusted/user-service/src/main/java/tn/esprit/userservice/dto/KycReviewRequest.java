package tn.esprit.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KycReviewRequest {

    @NotBlank(message = "Decision is required")
    private String decision; // APPROVED or REJECTED

    private String rejectReason;
}