package com.trustedwork.module06.entity;

import com.trustedwork.module06.enums.BadgeRarity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "badges")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Badge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private BadgeRarity rarity;

    private int xpReward;
    private String iconUrl;

    @OneToMany(mappedBy = "badge", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private java.util.List<UserBadge> userBadges;
}
