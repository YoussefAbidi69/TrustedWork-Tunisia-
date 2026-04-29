package tn.esprit.smartjobboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsDto {
    private long totalJobs;
    private long publishedJobs;
    private long totalApplications;
    private long flaggedJobs;
    private double avgMatchScore;
}
