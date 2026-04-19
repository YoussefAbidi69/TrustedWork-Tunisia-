package tn.esprit.mscontractservicee.dto.ai;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class MilestoneAiResponse {
    private String titre;
    private String description;
    private BigDecimal montant;
    private LocalDate deadline;
}
