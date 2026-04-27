package tn.esprit.userservice.service;



import tn.esprit.userservice.entity.CollaborationLog;

import java.util.List;

public interface ICollaborationLogServices {

    CollaborationLog addLog(Long agencyId, CollaborationLog log);

    List<CollaborationLog> getLogsByAgency(Long agencyId);

    List<CollaborationLog> getLogsByAgencyAndUser(Long agencyId, Long userId);

    CollaborationLog getLogById(Long id);

    void deleteLog(Long id);

    List<CollaborationLog> getLogsByUser(Long userId);
}