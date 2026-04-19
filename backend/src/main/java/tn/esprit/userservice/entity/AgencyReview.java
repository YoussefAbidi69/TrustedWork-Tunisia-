package tn.esprit.userservice.entity;



import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;


@Entity
@Table(name = "agency_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reviewerUserId;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 2000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewTargetType targetType;

    private Long projectId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime reviewedAt;



    @PrePersist
    public void prePersist() {
        if (this.reviewedAt == null) {
            this.reviewedAt = LocalDateTime.now();
        }

        if (this.targetType == null) {
            this.targetType = ReviewTargetType.AGENCY;
        }
    }

    // Relation ManyToOne avec Agency. Un avis appartient à une seule agence, mais une agence peut avoir plusieurs avis.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;
}