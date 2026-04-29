package tn.esprit.mscontractservicee.dto.signing;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SignatureRequestCreateResponse {
    private Long signatureRequestId;
    private String status;
    private boolean emailsSent;
}

