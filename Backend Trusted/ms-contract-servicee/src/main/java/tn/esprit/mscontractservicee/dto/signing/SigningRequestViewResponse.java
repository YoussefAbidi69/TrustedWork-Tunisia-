package tn.esprit.mscontractservicee.dto.signing;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class SigningRequestViewResponse {
    private Long signatureRequestId;
    private Long contractId;
    private String contractReference;
    private String requestStatus;
    private String signerRole;
    private String signerStatus;
    private LocalDateTime tokenExpiresAt;
    private String snapshotHash;
    private ContractSnapshot snapshot;
    private List<SigningSignerView> signers;
}

