package tn.esprit.msprojectservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.msprojectservice.dto.DeliveryRiskSignalDTO;
import tn.esprit.msprojectservice.entities.DeliveryRiskSignal;
import tn.esprit.msprojectservice.repositories.IRiskSignalRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskSignalServiceImpl implements IRiskSignalService {

    private final IRiskSignalRepository riskSignalRepository;

    @Override
    public List<DeliveryRiskSignalDTO> getActiveRisksByProjectId(Long projectId) {
        return riskSignalRepository.findByProjectIdAndResolvedFalse(projectId)
                .stream()
                .map(DeliveryRiskSignalDTO::fromEntity)
                .toList();
    }

    @Override
    public DeliveryRiskSignalDTO getLatestCriticalRisk(Long projectId) {
        DeliveryRiskSignal signal = riskSignalRepository.findLatestCriticalByProjectId(projectId)
                .orElseThrow(() -> new RuntimeException("Aucun signal de risque actif pour le projet : " + projectId));
        return DeliveryRiskSignalDTO.fromEntity(signal);
    }

    @Override
    public DeliveryRiskSignalDTO resolveRisk(Long id) {
        DeliveryRiskSignal signal = riskSignalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signal de risque non trouvé avec l'id : " + id));

        if (signal.isResolved()) {
            throw new IllegalStateException("Ce signal est déjà résolu");
        }

        signal.setResolved(true);
        signal.setResolvedAt(LocalDateTime.now());

        DeliveryRiskSignal updated = riskSignalRepository.save(signal);
        return DeliveryRiskSignalDTO.fromEntity(updated);
    }
}