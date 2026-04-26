package tn.esprit.mscontractservicee.dto.milestone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MilestoneCreateRequest {
    private Long contractId;
    private Integer ordre;
    private String titre;
    private String description;
    private BigDecimal montant;
    private LocalDate deadline;
}
