package tn.esprit.userservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.userservice.entity.CollaborationLog;

import java.util.List;

public interface ICollaborationLogRepository extends JpaRepository<CollaborationLog, Long> {

  List<CollaborationLog> findByAgencyIdOrderBySentAtDesc(Long agencyId);

  List<CollaborationLog> findByAgencyIdAndUserIdOrderBySentAtDesc(Long agencyId, Long userId);

  List<CollaborationLog> findByUserId(Long userId);
}