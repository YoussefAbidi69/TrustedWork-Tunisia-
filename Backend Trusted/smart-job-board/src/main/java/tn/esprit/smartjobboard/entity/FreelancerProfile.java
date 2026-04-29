package tn.esprit.smartjobboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Local cache of freelancer skills and contact email for matching and opportunity notifications.
 */
@Entity
@Table(name = "freelancer_profiles")
@Getter
@Setter
public class FreelancerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "freelancer_profile_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    @Column(name = "preferred_rate", precision = 14, scale = 2)
    private BigDecimal preferredRate;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }
}
