package tn.esprit.userservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.dto.chat.ReactionSummaryDTO;
import tn.esprit.userservice.entity.MessageReaction;
import tn.esprit.userservice.repository.MessageReactionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageReactionService {

    @Autowired
    private MessageReactionRepository reactionRepo;

    public void toggleReaction(Long messageId, Long userId, String emoji, boolean remove) {
        if (remove) {
            reactionRepo.deleteByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);
        } else {
            MessageReaction reaction = new MessageReaction();
            reaction.setMessageId(messageId);
            reaction.setUserId(userId);
            reaction.setEmoji(emoji);
            reaction.setReactedAt(LocalDateTime.now());
            try {
                reactionRepo.save(reaction);
            } catch (DataIntegrityViolationException ignored) {}
        }
    }

    public List<ReactionSummaryDTO> buildReactionSummary(Long messageId) {
        List<Object[]> raw = reactionRepo.findRawSummaryByMessageId(messageId);
        List<ReactionSummaryDTO> res = new ArrayList<>();
        for (Object[] row : raw) {
            String emoji = (String) row[0];
            int count = ((Number) row[1]).intValue();
            String userIdsStr = (String) row[2];
            List<Long> userIds = Arrays.stream(userIdsStr.split(","))
                                       .map(Long::valueOf)
                                       .collect(Collectors.toList());
            res.add(new ReactionSummaryDTO(emoji, count, userIds, false));
        }
        return res;
    }

    public Map<Long, List<ReactionSummaryDTO>> loadReactionSummaries(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return Map.of();
        List<Object[]> raw = reactionRepo.findRawSummaryByMessageIds(messageIds);
        return raw.stream().collect(Collectors.groupingBy(
            row -> ((Number) row[0]).longValue(),
            Collectors.mapping(row -> {
                String emoji = (String) row[1];
                int count = ((Number) row[2]).intValue();
                String userIdsStr = (String) row[3];
                List<Long> userIds = Arrays.stream(userIdsStr.split(","))
                                           .map(Long::valueOf)
                                           .collect(Collectors.toList());
                return new ReactionSummaryDTO(emoji, count, userIds, false);
            }, Collectors.toList())
        ));
    }
}
