package tn.esprit.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.userservice.entity.MessageReaction;

import java.util.List;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM MessageReaction r WHERE r.messageId = :messageId AND r.userId = :userId AND r.emoji = :emoji")
    void deleteByMessageIdAndUserIdAndEmoji(@Param("messageId") Long messageId, @Param("userId") Long userId, @Param("emoji") String emoji);

    @Query(value = "SELECT emoji, COUNT(*) as cnt, GROUP_CONCAT(user_id) as user_ids FROM message_reactions WHERE message_id = :messageId GROUP BY emoji", nativeQuery = true)
    List<Object[]> findRawSummaryByMessageId(@Param("messageId") Long messageId);

    @Query(value = "SELECT message_id, emoji, COUNT(*) as cnt, GROUP_CONCAT(user_id) as user_ids FROM message_reactions WHERE message_id IN :messageIds GROUP BY message_id, emoji", nativeQuery = true)
    List<Object[]> findRawSummaryByMessageIds(@Param("messageIds") List<Long> messageIds);
}
