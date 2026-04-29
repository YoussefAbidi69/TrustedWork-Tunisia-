package tn.esprit.smartjobboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketSkillInsightDto {
    private String skill;
    private long count;
    private TrendDirection trend;
    /** vs previous 30-day window, percentage change */
    private double changePercent;
    /** count in the previous 30-day window */
    private long lastPeriodCount;
}
