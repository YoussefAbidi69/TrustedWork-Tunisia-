package com.trustedwork.module06.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "streaks")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Streak {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long userId;

    private int currentStreak;
    private int longestStreak;
    private LocalDate lastActivityDate;
}
