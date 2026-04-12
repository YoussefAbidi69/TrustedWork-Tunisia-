package tn.esprit.mscontractservicee.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import tn.esprit.mscontractservicee.enums.EscrowStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "escrow_accounts")
public class EscrowAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long contractId;
    BigDecimal montantBloque;
    BigDecimal montantLibere;
    BigDecimal montantTotal;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    EscrowStatus status = EscrowStatus.LOCKED;

    LocalDateTime lockedAt;
    LocalDateTime releasedAt;
    LocalDateTime refundedAt;
    LocalDateTime updatedAt;

    // Relations
    @OneToOne
    @JoinColumn(name = "contractId", insertable = false, updatable = false)
    @ToString.Exclude
    private Contract contract;

    // ✅ AJOUTER LA RELATION AVEC TRANSACTION
    @OneToMany(mappedBy = "escrowAccount", cascade = CascadeType.ALL)
    @ToString.Exclude
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();
}