package tn.esprit.mscontractservicee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import tn.esprit.mscontractservicee.enums.SignerRole;
import tn.esprit.mscontractservicee.enums.SignatureSignerStatus;
import tn.esprit.mscontractservicee.enums.SignatureType;

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
public class SignatureSigner implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long signatureRequestId;

    @Enumerated(EnumType.STRING)
    SignerRole role;

    Long signerCin;

    String signerEmail;

    // SHA-256 hex of the raw token sent by email (token itself is never stored)
    String tokenHash;

    LocalDateTime tokenExpiresAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    SignatureSignerStatus status = SignatureSignerStatus.PENDING;

    LocalDateTime signedAt;

    @Enumerated(EnumType.STRING)
    SignatureType signatureType;

    // For DRAWN: base64 PNG data URL or raw base64; For TYPED: typed name.
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    String signaturePayload;

    String documentHash;
    String ip;

    @Column(length = 512)
    String userAgent;

    @ManyToOne
    @JoinColumn(name = "signatureRequestId", insertable = false, updatable = false)
    @ToString.Exclude
    @JsonIgnore
    SignatureRequest signatureRequest;
}

