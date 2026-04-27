package tn.esprit.mscontractservicee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.mscontractservicee.entity.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Récupérer toutes les notifications d'un utilisateur, triées par les plus récentes
    List<Notification> findByRecipientCinOrderByCreatedAtDesc(Long recipientCin);
    
    // Récupérer uniquement les notifications non lues
    List<Notification> findByRecipientCinAndReadFalseOrderByCreatedAtDesc(Long recipientCin);
    
    // Compter le nombre de notifications non lues
    long countByRecipientCinAndReadFalse(Long recipientCin);
}
