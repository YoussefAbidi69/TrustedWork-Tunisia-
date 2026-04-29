package tn.esprit.smartjobboard.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class JobOfferCreateRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    @Size(max = 120)
    private String category;

    private List<@NotBlank @Size(max = 120) String> requiredSkills = new ArrayList<>();

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal budgetMin;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal budgetMax;

    private Integer durationDays;

    @Size(max = 255)
    private String location;

    private boolean remote;

    private LocalDateTime expiresAt;
}
