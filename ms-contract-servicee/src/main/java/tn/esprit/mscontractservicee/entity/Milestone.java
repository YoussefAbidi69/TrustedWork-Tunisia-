package tn.esprit.mscontractservicee.entity;

import tn.esprit.mscontractservicee.enums.MilestoneStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Milestone implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long contractId;
    Integer ordre;
    String titre;
    String description;
    BigDecimal montant;
    LocalDate deadline;
    LocalDateTime startedAt;
    LocalDateTime submittedAt;
    LocalDateTime validatedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    MilestoneStatus status = MilestoneStatus.PENDING;

    String rejectionReason;

    @ManyToOne
    @JoinColumn(name = "contractId", insertable = false, updatable = false)
    @ToString.Exclude
    @JsonIgnore  // ✅ IGNORE LA REFERENCE VERS CONTRACT
    private Contract contract;

    @OneToOne(mappedBy = "milestone", cascade = CascadeType.ALL)
    @ToString.Exclude
    @JsonIgnore  // ✅ IGNORE LA REFERENCE VERS DELIVERYPROOF (si elle existe)
    private DeliveryProof deliveryProof;
}