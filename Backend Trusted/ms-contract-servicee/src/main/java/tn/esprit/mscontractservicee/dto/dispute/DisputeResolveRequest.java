package tn.esprit.mscontractservicee.dto.dispute;

import lombok.Getter;
import lombok.Setter;
import tn.esprit.mscontractservicee.enums.DisputeStatus;

import java.math.BigDecimal;

@Getter
@Setter
public class DisputeResolveRequest {
    private DisputeStatus status;
    private String decision;
    private BigDecimal montantRembourse;
    private BigDecimal montantLibere;
}

