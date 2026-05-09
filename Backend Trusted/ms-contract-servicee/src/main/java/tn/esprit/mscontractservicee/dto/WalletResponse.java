package tn.esprit.mscontractservicee.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletResponse {
    Long id;
    Long userCin;
    BigDecimal balance;
    BigDecimal totalEarned;
    BigDecimal totalSpent;
    BigDecimal totalCommissionPaid;
    String stripeAccountId;
    String stripeAccountStatus;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
