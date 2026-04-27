package com.trustedwork.module06.repository;
import com.trustedwork.module06.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUserId(Long userId);
    List<UserBadge> findByBadgeId(Long badgeId);
    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);
}
