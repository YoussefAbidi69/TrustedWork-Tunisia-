package tn.esprit.smartjobboard.dto;

import lombok.Builder;
import lombok.Data;
import tn.esprit.smartjobboard.entity.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class JobApplicationResponse {
    private Long id;
    private Long jobOfferId;
    private String jobTitle;
    private String jobCategory;
    private String jobLocation;
    private String jobStatus;
    private Long freelancerId;
    private String coverLetter;
    private BigDecimal proposedRate;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private MatchScoreBreakdownDto matchScore;
    private Double successProbability;
    private String predictionConfidence;
}
