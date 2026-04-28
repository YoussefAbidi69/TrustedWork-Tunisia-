package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.entity.Agency;
import tn.esprit.userservice.entity.CollaborationLog;
import tn.esprit.userservice.repository.IAgencyRepository;
import tn.esprit.userservice.repository.ICollaborationLogRepository;
import tn.esprit.userservice.service.ICollaborationLogServices;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollaborationLogServiceImpl implements ICollaborationLogServices {

    private final ICollaborationLogRepository collaborationLogRepository;
    private final IAgencyRepository agencyRepository;

    @Override
    public CollaborationLog addLog(Long agencyId, CollaborationLog log) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agency not found"));

        log.setAgency(agency);

        return collaborationLogRepository.save(log);
    }

    @Override
    public List<CollaborationLog> getLogsByAgency(Long agencyId) {
        return collaborationLogRepository.findByAgencyIdOrderBySentAtDesc(agencyId);
    }

    @Override
    public List<CollaborationLog> getLogsByAgencyAndUser(Long agencyId, Long userId) {
        return collaborationLogRepository.findByAgencyIdAndSenderIdOrderBySentAtDesc(agencyId, userId);
    }

    @Override
    public CollaborationLog getLogById(Long id) {
        return collaborationLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Collaboration log not found"));
    }

    @Override
    public void deleteLog(Long id) {
        collaborationLogRepository.deleteById(id);
    }

    @Override
    public List<CollaborationLog> getLogsByUser(Long userId) {
        return collaborationLogRepository.findBySenderId(userId);
    }
}