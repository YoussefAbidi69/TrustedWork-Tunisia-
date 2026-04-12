package tn.esprit.mscontractservicee.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import tn.esprit.mscontractservicee.enums.PaymentMethod;
import tn.esprit.mscontractservicee.enums.TransactionStatus;
import tn.esprit.mscontractservicee.enums.TransactionType;

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
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String reference;

    Long escrowId;
    Long milestoneId;
    Long contractId;
    Long walletId;

    @Enumerated(EnumType.STRING)
    TransactionType type;

    BigDecimal montant;
    BigDecimal commissionDynamique;
    BigDecimal montantCommission;
    BigDecimal montantNet;

    @Enumerated(EnumType.STRING)
    PaymentMethod methodePaiement;

    String facturePdfUrl;
    String description;

    // Stripe fields
    String stripePaymentIntentId;
    String stripeTransferId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    TransactionStatus status = TransactionStatus.PENDING;

    LocalDateTime processedAt;
    String failureReason;
    LocalDateTime createdAt;

    // ==================== RELATIONS ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractId", insertable = false, updatable = false)
    @ToString.Exclude
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "walletId", insertable = false, updatable = false)
    @ToString.Exclude
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escrowId", insertable = false, updatable = false)
    @ToString.Exclude
    private EscrowAccount escrowAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestoneId", insertable = false, updatable = false)
    @ToString.Exclude
    private Milestone milestone;
}