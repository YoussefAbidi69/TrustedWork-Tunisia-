package com.trustedwork.module06.entity;

import com.trustedwork.module06.enums.ParticipationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "challenge_participations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"challenge_id", "user_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ChallengeParticipation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private ParticipationStatus status;

    @CreationTimestamp
    private LocalDateTime joinedAt;

    private LocalDateTime completedAt;
}
