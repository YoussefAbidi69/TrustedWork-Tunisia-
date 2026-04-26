package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.freelancerprofileservice.entities.Notification;

import java.util.List;

/**
 * Repository JPA pour les notifications persistées.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Récupérer toutes les notifications non lues d'un utilisateur (plus récentes en premier)
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    // Compter les non-lues (pour le badge)
    long countByUserIdAndIsReadFalse(Long userId);

    // Marquer toutes les notifications d'un user comme lues
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);
}