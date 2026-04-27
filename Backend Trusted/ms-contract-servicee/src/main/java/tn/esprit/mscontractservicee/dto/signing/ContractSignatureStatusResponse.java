package tn.esprit.mscontractservicee.dto.signing;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ContractSignatureStatusResponse {
    private Long contractId;
    private String contractStatus;
    private LocalDateTime contractSignedAt;

    private Long signatureRequestId;
    private String signatureRequestStatus;
    private LocalDateTime signatureRequestCreatedAt;
    private LocalDateTime signatureRequestCompletedAt;

    private boolean fullySigned;
    private List<SignerStatus> signers;

    @Getter
    @Setter
    @Builder
    public static class SignerStatus {
        private String role;
        private String email;
        private String status;
        private LocalDateTime signedAt;
    }
}

