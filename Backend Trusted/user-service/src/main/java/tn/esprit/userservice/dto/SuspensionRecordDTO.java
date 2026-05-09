package tn.esprit.userservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SuspensionRecordDTO {

    private Long id;
    private Long userId;
    private String userEmail;

    // Suspension
    private String reason;
    private String suspendedBy;
    private LocalDateTime suspendedAt;

    // Levée
    private LocalDateTime liftedAt;
    private String liftedBy;

    // Statut
    private boolean active;
}