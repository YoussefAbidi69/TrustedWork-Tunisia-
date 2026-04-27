package tn.esprit.mscontractservicee.service;

import tn.esprit.mscontractservicee.dto.ai.ContractAiPromptRequest;
import tn.esprit.mscontractservicee.dto.ai.ContractAiResponse;
import tn.esprit.mscontractservicee.dto.ai.MilestoneAiPromptRequest;
import tn.esprit.mscontractservicee.dto.ai.MilestoneAiResponse;

public interface IContractAiGenerationService {
    ContractAiResponse generateContractDraft(ContractAiPromptRequest request);
    MilestoneAiResponse generateMilestoneDraft(MilestoneAiPromptRequest request);
}
