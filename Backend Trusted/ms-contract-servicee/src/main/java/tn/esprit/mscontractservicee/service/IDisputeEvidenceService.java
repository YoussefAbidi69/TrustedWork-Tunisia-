package tn.esprit.mscontractservicee.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.mscontractservicee.dto.dispute.DisputeEvidenceResponse;

import java.util.List;

public interface IDisputeEvidenceService {

    DisputeEvidenceResponse uploadEvidence(Long disputeId, Long authenticatedCin, boolean admin, MultipartFile file);

    List<DisputeEvidenceResponse> listEvidence(Long disputeId, Long authenticatedCin, boolean admin);

    EvidenceDownload downloadEvidence(Long disputeId, Long evidenceId, Long authenticatedCin, boolean admin);

    record EvidenceDownload(DisputeEvidenceResponse meta, Resource resource) {}
}

