package tn.esprit.mscontractservicee.service;

import tn.esprit.mscontractservicee.dto.dispute.DisputeAssignRequest;
import tn.esprit.mscontractservicee.dto.dispute.DisputeCreateRequest;
import tn.esprit.mscontractservicee.dto.dispute.DisputeResolveRequest;
import tn.esprit.mscontractservicee.dto.dispute.DisputeRespondRequest;
import tn.esprit.mscontractservicee.entity.Dispute;

import java.util.List;

public interface IDisputeService {
    Dispute openDispute(Long authenticatedCin, DisputeCreateRequest request);
    Dispute respond(Long disputeId, Long authenticatedCin, boolean admin, DisputeRespondRequest request);
    Dispute assign(Long disputeId, Long adminCin, DisputeAssignRequest request);
    Dispute resolve(Long disputeId, Long adminCin, DisputeResolveRequest request);

    Dispute getByIdForUser(Long disputeId, Long authenticatedCin, boolean admin);
    List<Dispute> listByContractForUser(Long contractId, Long authenticatedCin, boolean admin);
    List<Dispute> listByMilestoneForUser(Long milestoneId, Long authenticatedCin, boolean admin);
}
