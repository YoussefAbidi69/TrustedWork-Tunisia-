package tn.esprit.userservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.userservice.entity.CollaborationLog;

import java.util.List;

public interface ICollaborationLogRepository extends JpaRepository<CollaborationLog, Long> {

  List<CollaborationLog> findByAgencyIdOrderBySentAtDesc(Long agencyId);

  List<CollaborationLog> findByAgencyIdAndSenderIdOrderBySentAtDesc(Long agencyId, Long senderId);

  org.springframework.data.domain.Page<CollaborationLog> findByAgencyIdOrderBySentAtDesc(Long agencyId, org.springframework.data.domain.Pageable pageable);

  @org.springframework.data.jpa.repository.Query(value = "SELECT cl FROM CollaborationLog cl " +
      "LEFT JOIN FETCH cl.replyTo rt " +
      "LEFT JOIN FETCH cl.sender s " +
      "WHERE cl.agency.id = :agencyId AND cl.isDeleted = false " +
      "ORDER BY cl.sentAt DESC",
      countQuery = "SELECT count(cl) FROM CollaborationLog cl WHERE cl.agency.id = :agencyId AND cl.isDeleted = false")
  org.springframework.data.domain.Page<CollaborationLog> findByAgencyIdWithReplies(@org.springframework.data.repository.query.Param("agencyId") Long agencyId, org.springframework.data.domain.Pageable pageable);

  @org.springframework.data.jpa.repository.Query("SELECT cl FROM CollaborationLog cl " +
      "LEFT JOIN FETCH cl.replyTo rt " +
      "LEFT JOIN FETCH cl.sender s " +
      "WHERE cl.agency.id = :agencyId AND cl.isPinned = true AND cl.isDeleted = false " +
      "ORDER BY cl.pinnedAt DESC")
  List<CollaborationLog> findPinnedMessagesByAgencyId(@org.springframework.data.repository.query.Param("agencyId") Long agencyId);

  List<CollaborationLog> findBySenderId(Long senderId);
}