package com.trustedwork.module06.repository;
import com.trustedwork.module06.entity.GrowthProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GrowthProfileRepository extends JpaRepository<GrowthProfile, Long> {
    Optional<GrowthProfile> findByUserId(Long userId);
}
