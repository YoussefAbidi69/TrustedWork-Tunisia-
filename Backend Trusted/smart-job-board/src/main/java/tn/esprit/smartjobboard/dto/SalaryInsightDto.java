package tn.esprit.smartjobboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryInsightDto {
    private String skill;
    private double avgProposedRate;
    private double minRate;
    private double maxRate;
    private double medianRate;
    private long sampleCount;
    private String category;
}
