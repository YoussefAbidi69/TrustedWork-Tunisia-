package tn.esprit.community.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.enums.VoteType;
import tn.esprit.community.entity.Vote;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findByPostIdAndUserId(Long postId, Long userId);

    List<Vote> findByUserIdAndPostIdIn(Long userId, Collection<Long> postIds);

    long countByPostIdAndType(Long postId, VoteType type);
}
