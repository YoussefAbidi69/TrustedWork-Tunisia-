package com.trustedwork.module06.entity;

import com.trustedwork.module06.enums.ChallengeStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "challenges")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Challenge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private int xpReward;
    private LocalDateTime deadline;

    private String challengeTypeCode; // e.g. REG_EVENT, FIRST_BADGE

    @Enumerated(EnumType.STRING)
    private ChallengeStatus status;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private java.util.List<ChallengeParticipation> participations;
}
