package com.trustedwork.module06.repository;
import com.trustedwork.module06.entity.Streak;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StreakRepository extends JpaRepository<Streak, Long> {
    Optional<Streak> findByUserId(Long userId);
}
