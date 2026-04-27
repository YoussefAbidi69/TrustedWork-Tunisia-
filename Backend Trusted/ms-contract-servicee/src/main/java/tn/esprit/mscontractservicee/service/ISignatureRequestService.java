package tn.esprit.mscontractservicee.service;

import tn.esprit.mscontractservicee.dto.signing.SignatureRequestCreateResponse;
import tn.esprit.mscontractservicee.dto.signing.SigningRequestViewResponse;
import tn.esprit.mscontractservicee.dto.signing.SigningSignRequest;

public interface ISignatureRequestService {
    SignatureRequestCreateResponse createAndSendForContract(Long contractId);
    SigningRequestViewResponse viewForToken(Long signatureRequestId, String token);
    void sign(Long signatureRequestId, SigningSignRequest request, String ip, String userAgent);
}

