package tn.esprit.community.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.Enum.VoteType;
import tn.esprit.community.entity.Vote;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findByPost_IdAndUserId(Long postId, Long userId);

    List<Vote> findByUserIdAndPost_IdIn(Long userId, Collection<Long> postIds);

    long countByPost_IdAndType(Long postId, VoteType type);
}
