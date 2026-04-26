package com.trustedwork.module06.repository;
import com.trustedwork.module06.entity.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LeaderboardRepository extends JpaRepository<Leaderboard, Long> {
    Optional<Leaderboard> findByUserId(Long userId);
    List<Leaderboard> findByGovernorateOrderByEngagementRankAsc(String governorate);
    List<Leaderboard> findAllByOrderByEngagementRankAsc();
}
