package tn.esprit.mscontractservicee.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
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
public class WalletTransactionResponse {
    Long id;
    String reference;
    TransactionType type;
    BigDecimal montant;
    String description;
    TransactionStatus status;
    LocalDateTime createdAt;
}

