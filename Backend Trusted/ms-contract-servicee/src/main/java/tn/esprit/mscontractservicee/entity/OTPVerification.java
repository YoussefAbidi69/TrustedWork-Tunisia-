package tn.esprit.mscontractservicee.entity;

import tn.esprit.mscontractservicee.enums.OTPStatus;
import tn.esprit.mscontractservicee.enums.OTPType;
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
public class OTPVerification implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long contractId;
    @Column(name = "user_cin")
    Long userCin;
    String otpCode;

    @Enumerated(EnumType.STRING)
    OTPType type;

    LocalDateTime expiresAt;
    LocalDateTime verifiedAt;

    @Builder.Default
    Integer attempts = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    OTPStatus status = OTPStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "contractId", insertable = false, updatable = false)
    @ToString.Exclude
    Contract contract;
}
