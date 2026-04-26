package tn.esprit.mscontractservicee.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
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
@Table(name = "wallet")
public class Wallet implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_cin", unique = true, nullable = false)
    Long userCin;

    @Column(nullable = false)
    @Builder.Default
    BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    BigDecimal totalEarned = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    BigDecimal totalCommissionPaid = BigDecimal.ZERO;

    @Column(unique = true)
    String stripeAccountId;

    String stripeAccountStatus;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    // ✅ AJOUTER LA RELATION AVEC TRANSACTION
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL)
    @ToString.Exclude
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();
}
