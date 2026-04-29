package tn.esprit.smartjobboard.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ApplicationCreateRequest {

    @NotNull
    private Long jobOfferId;

    @NotBlank
    @Size(min = 10, max = 8000)
    private String coverLetter;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal proposedRate;

    /**
     * Optional skills declared by the applicant; merged into {@code FreelancerProfile} for matching.
     */
    @JsonAlias("freelancerSkills")
    private List<@NotBlank @Size(max = 120) String> declaredSkills = new ArrayList<>();
}
