package tn.esprit.smartjobboard.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminMarketAnalyticsResponse {
    private List<MarketSkillInsightDto> skillInsights;
    private long totalPublishedJobs;
    private double platformMockAverageBudget;
}
