package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;
import tn.esprit.freelancerprofileservice.enums.ReportCategory;
import tn.esprit.freelancerprofileservice.enums.ReportStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class ProfileReportResponse {

    private Long id;

    private Long reporterId;
    private String reporterName;

    private Long profileId;
    private Long freelancerUserId;
    private String freelancerName;

    private ReportCategory category;
    private String description;
    private ReportStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    private Integer riskScore;
    private Boolean suspended;
}