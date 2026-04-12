package tn.esprit.msprojectservice.services;

import tn.esprit.msprojectservice.dto.DeliveryRiskSignalDTO;

import java.util.List;

public interface IRiskSignalService {

    List<DeliveryRiskSignalDTO> getActiveRisksByProjectId(Long projectId);

    DeliveryRiskSignalDTO getLatestCriticalRisk(Long projectId);

    DeliveryRiskSignalDTO resolveRisk(Long id);
}