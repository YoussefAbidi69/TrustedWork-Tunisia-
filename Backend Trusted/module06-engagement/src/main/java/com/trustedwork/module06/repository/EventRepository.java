package com.trustedwork.module06.repository;
import com.trustedwork.module06.entity.Event;
import com.trustedwork.module06.enums.EventStatus;
import com.trustedwork.module06.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByGovernorate(String governorate);
    List<Event> findByStatus(EventStatus status);
    List<Event> findByTypeAndStatus(EventType type, EventStatus status);
}
