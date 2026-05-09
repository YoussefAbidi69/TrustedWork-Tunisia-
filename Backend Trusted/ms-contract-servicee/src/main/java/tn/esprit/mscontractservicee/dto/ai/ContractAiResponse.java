package tn.esprit.mscontractservicee.dto.ai;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ContractAiResponse {
    private String projectTitle;
    private String description;
    private BigDecimal montantTotal;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Integer slaFreelancerHeures;
    private Integer slaClientJours;
}
