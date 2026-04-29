package tn.esprit.smartjobboard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tn.esprit.smartjobboard.entity.ApplicationStatus;

@Data
public class ApplicationStatusUpdateRequest {
    @NotNull
    private ApplicationStatus status;
}
