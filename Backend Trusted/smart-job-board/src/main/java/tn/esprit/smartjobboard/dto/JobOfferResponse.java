package tn.esprit.smartjobboard.dto;

import lombok.Builder;
import lombok.Data;
import tn.esprit.smartjobboard.entity.JobOfferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class JobOfferResponse {
    private Long id;
    private Long clientId;
    private String title;
    private String description;
    private String category;
    private List<String> requiredSkills;
    private List<String> extractedSkills;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private Integer durationDays;
    private String location;
    private boolean remote;
    private JobOfferStatus status;
    private double fraudRiskScore;
    private double opportunityScore;
    private Double opportunityBudgetComponent;
    private Double opportunityDemandComponent;
    private Double opportunityCompetitionComponent;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FraudSignalDto> fraudSignals;
    /** Number of applications submitted for this job */
    private Long applicationCount;
}
