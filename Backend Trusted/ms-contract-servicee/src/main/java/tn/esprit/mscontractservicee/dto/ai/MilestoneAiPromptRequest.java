package tn.esprit.mscontractservicee.dto.ai;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MilestoneAiPromptRequest {
    private String prompt;
    private String contractTitle;
    private String contractDescription;
    private BigDecimal remainingBudget;
    private String contractDeadline;
    private java.util.List<String> existingMilestones;
}
