package tn.esprit.smartjobboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.smartjobboard.entity.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    java.util.Optional<Message> findTopByConversationIdOrderByCreatedAtDesc(String conversationId);

    /**
     * Full conversation between two users for a specific job, ordered chronologically.
     */
    @Query("""
        SELECT m FROM Message m
        WHERE m.conversationId = :conversationId
        ORDER BY m.createdAt ASC
    """)
    List<Message> findConversation(@Param("conversationId") String conversationId);

    /**
     * All messages where the user is sender or receiver (for listing conversations).
     */
    @Query("""
        SELECT m FROM Message m
        WHERE m.senderId = :userId OR m.receiverId = :userId
        ORDER BY m.createdAt DESC
    """)
    List<Message> findAllForUser(@Param("userId") Long userId);

    @Modifying
    @Query("""
        UPDATE Message m
        SET m.conversationId = :conversationId
        WHERE m.jobOfferId = :jobOfferId
          AND ((m.senderId = :userA AND m.receiverId = :userB)
            OR (m.senderId = :userB AND m.receiverId = :userA))
          AND (m.conversationId IS NULL OR m.conversationId = '')
    """)
    int backfillConversationId(@Param("conversationId") String conversationId,
                               @Param("jobOfferId") Long jobOfferId,
                               @Param("userA") Long userA,
                               @Param("userB") Long userB);

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
        WHERE m.conversationId = :conversationId
          AND m.senderId = :peerId
          AND m.receiverId = :currentUserId
          AND m.isRead = false
    """)
    int markConversationRead(@Param("conversationId") String conversationId,
                              @Param("peerId") Long peerId,
                              @Param("currentUserId") Long currentUserId);
}
