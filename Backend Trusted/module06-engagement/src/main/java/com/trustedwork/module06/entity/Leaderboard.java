package com.trustedwork.module06.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboard")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Leaderboard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long userId;

    private String governorate;
    private double engagementScore;

    @Column(name = "engagement_rank")
    private Integer engagementRank;

    @UpdateTimestamp
    private LocalDateTime computedAt;
}
