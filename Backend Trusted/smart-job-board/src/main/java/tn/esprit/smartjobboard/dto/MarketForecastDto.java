package tn.esprit.smartjobboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketForecastDto {
    private String skill;
    private long currentDemand;
    private long forecastNextMonth;
    private long forecastIn3Months;
    private String confidence;
}
