package com.trustedwork.module06.repository;
import com.trustedwork.module06.entity.Challenge;
import com.trustedwork.module06.enums.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    List<Challenge> findByStatus(ChallengeStatus status);
}
