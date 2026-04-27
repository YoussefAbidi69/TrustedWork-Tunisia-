package tn.esprit.mscontractservicee.entity;

import tn.esprit.mscontractservicee.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class DeliveryProof implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long milestoneId;
    String fichiers;
    String lienDemo;
    String repoGit;
    String commentaire;
    String hashMD5;
    LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    DeliveryStatus status = DeliveryStatus.SUBMITTED;

    LocalDateTime approvedAt;
    @Column(name = "approved_by_cin")
    Long approvedByCin;

    @OneToOne
    @JoinColumn(name = "milestoneId", insertable = false, updatable = false)
    @ToString.Exclude
    Milestone milestone;
}
