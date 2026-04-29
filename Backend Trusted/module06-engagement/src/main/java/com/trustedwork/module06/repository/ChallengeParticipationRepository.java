package com.trustedwork.module06.repository;

import com.trustedwork.module06.entity.ChallengeParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChallengeParticipationRepository extends JpaRepository<ChallengeParticipation, Long> {
    Optional<ChallengeParticipation> findByUserIdAndChallengeId(Long userId, Long challengeId);
    List<ChallengeParticipation> findByUserId(Long userId);
}
