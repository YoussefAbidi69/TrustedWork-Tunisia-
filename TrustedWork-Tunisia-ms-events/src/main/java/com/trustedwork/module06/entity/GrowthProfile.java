package com.trustedwork.module06.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "growth_profiles")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GrowthProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long userId;

    private int xpPoints;
    private int level;
    private double engagementScore;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
