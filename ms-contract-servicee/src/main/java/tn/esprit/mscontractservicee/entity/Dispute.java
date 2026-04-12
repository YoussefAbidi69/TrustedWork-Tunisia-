package tn.esprit.mscontractservicee.entity;

import tn.esprit.mscontractservicee.enums.DisputeStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Dispute implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String reference;
    Long contractId;
    Long milestoneId;
    Long plaignantId;
    Long defendantId;
    Long arbitreId;
    String motif;
    String preuvesPlaignant;
    String preuvesDefense;
    String decision;
    BigDecimal montantRembourse;
    BigDecimal montantLibere;
    LocalDateTime openedAt;
    LocalDateTime assignedAt;
    LocalDateTime resolvedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    DisputeStatus status = DisputeStatus.OPEN;

    @ManyToOne
    @JoinColumn(name = "contractId", insertable = false, updatable = false)
    @ToString.Exclude
    Contract contract;

    @ManyToOne
    @JoinColumn(name = "milestoneId", insertable = false, updatable = false)
    @ToString.Exclude
    Milestone milestone;
}