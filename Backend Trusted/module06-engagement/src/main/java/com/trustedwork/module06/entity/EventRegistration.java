package com.trustedwork.module06.entity;

import com.trustedwork.module06.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_registrations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EventRegistration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus status;

    @CreationTimestamp
    private LocalDateTime registeredAt;
}
