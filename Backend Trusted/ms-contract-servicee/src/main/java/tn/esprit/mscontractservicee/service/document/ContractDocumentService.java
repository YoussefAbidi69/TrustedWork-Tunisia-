package tn.esprit.mscontractservicee.service.document;

import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.entity.SignatureSigner;

import java.util.List;

public interface ContractDocumentService {
    default byte[] generateContractPdf(Contract contract, List<Milestone> milestones) {
        return generateContractPdf(contract, milestones, List.of());
    }

    byte[] generateContractPdf(Contract contract, List<Milestone> milestones, List<SignatureSigner> signers);
}
