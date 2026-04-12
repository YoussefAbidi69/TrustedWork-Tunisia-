package tn.esprit.msprojectservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.msprojectservice.entities.Notification;

import java.util.List;

public interface INotificationRepository extends JpaRepository<Notification, Long> {

    // Toutes les notifications d'un utilisateur (les plus récentes d'abord)
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Notifications non lues d'un utilisateur
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    // Compter les notifications non lues
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.read = false")
    int countUnreadByUserId(@Param("userId") Long userId);

    // Marquer toutes les notifications d'un utilisateur comme lues
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);
}