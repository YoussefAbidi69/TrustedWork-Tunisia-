package tn.esprit.mscontractservicee.dto.dispute;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DisputeCreateRequest {
    private Long contractId;
    // Optional: if null => contract-level dispute.
    private Long milestoneId;
    private String motif;
    private String preuvesPlaignant;
}
