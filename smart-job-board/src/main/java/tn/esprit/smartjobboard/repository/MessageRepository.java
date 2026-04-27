package tn.esprit.smartjobboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.smartjobboard.entity.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Full conversation between two users for a specific job, ordered chronologically.
     */
    @Query("""
        SELECT m FROM Message m
        WHERE m.jobOfferId = :jobId
          AND ((m.senderId = :userA AND m.receiverId = :userB)
            OR (m.senderId = :userB AND m.receiverId = :userA))
        ORDER BY m.sentAt ASC
    """)
    List<Message> findConversation(@Param("jobId") Long jobId,
                                   @Param("userA") Long userA,
                                   @Param("userB") Long userB);

    /**
     * All messages where the user is sender or receiver (for listing conversations).
     */
    @Query("""
        SELECT m FROM Message m
        WHERE m.senderId = :userId OR m.receiverId = :userId
        ORDER BY m.sentAt DESC
    """)
    List<Message> findAllForUser(@Param("userId") Long userId);

    /**
     * Count unread messages for a user.
     */
    long countByReceiverIdAndIsReadFalse(Long receiverId);

    /**
     * Mark all messages from a peer as read for a specific job conversation.
     */
    @Modifying
    @Query("""
        UPDATE Message m SET m.isRead = true
        WHERE m.jobOfferId = :jobId
          AND m.senderId = :peerId
          AND m.receiverId = :currentUserId
          AND m.isRead = false
    """)
    int markConversationRead(@Param("jobId") Long jobId,
                              @Param("peerId") Long peerId,
                              @Param("currentUserId") Long currentUserId);
}
