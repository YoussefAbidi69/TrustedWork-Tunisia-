package tn.esprit.mscontractservicee.dto.signing;

import lombok.Getter;
import lombok.Setter;
import tn.esprit.mscontractservicee.enums.SignatureType;

@Getter
@Setter
public class SigningSignRequest {
    private String token;
    private SignatureType signatureType;
    private String signaturePayload;
}

