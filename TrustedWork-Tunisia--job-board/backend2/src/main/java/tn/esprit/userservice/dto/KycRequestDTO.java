package tn.esprit.userservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class KycRequestDTO {

    private Long id;
    private Long userId;
    private String userEmail;

    // Documents
    private String cinDocumentPath;
    private String diplomaDocumentPath;
    private String selfiePath;

    // Liveness
    private Double livenessScore;
    private boolean livenessPassed;

    // Statut
    private String status;

    // Review admin
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String rejectReason;

    // Date
    private LocalDateTime createdAt;
}