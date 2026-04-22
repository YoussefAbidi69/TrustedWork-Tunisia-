package com.trustedwork.module06.repository;
import com.trustedwork.module06.entity.EventRegistration;
import com.trustedwork.module06.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    List<EventRegistration> findByUserId(Long userId);
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
    long countByEventId(Long eventId);
    long countByUserId(Long userId);
    List<EventRegistration> findByUserIdAndStatus(Long userId, RegistrationStatus status);
    java.util.List<EventRegistration> findByEventId(Long eventId);
}
