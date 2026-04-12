package tn.esprit.mscontractservicee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import tn.esprit.mscontractservicee.enums.SignatureRequestStatus;

import java.io.Serializable;
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
public class SignatureRequest implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long contractId;

    Integer contractVersion;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    String snapshotJson;

    // SHA-256 hex of snapshotJson (UTF-8)
    String snapshotHash;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    SignatureRequestStatus status = SignatureRequestStatus.CREATED;

    LocalDateTime createdAt;
    LocalDateTime sentAt;
    LocalDateTime completedAt;

    @OneToMany(mappedBy = "signatureRequest", cascade = CascadeType.ALL)
    @ToString.Exclude
    @Builder.Default
    @JsonIgnore
    List<SignatureSigner> signers = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "contractId", insertable = false, updatable = false)
    @ToString.Exclude
    @JsonIgnore
    Contract contract;
}

