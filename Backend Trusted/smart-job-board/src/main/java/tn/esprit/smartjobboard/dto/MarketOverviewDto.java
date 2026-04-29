package tn.esprit.smartjobboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketOverviewDto {
    private long activeJobPostings;
    private double avgCompetition;
}
