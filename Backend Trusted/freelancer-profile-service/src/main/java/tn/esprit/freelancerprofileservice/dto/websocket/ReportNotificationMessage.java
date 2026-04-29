package tn.esprit.freelancerprofileservice.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message envoyé en temps réel via WebSocket pour le module de réclamation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportNotificationMessage {

    private String type;        // NEW_REPORT / PROFILE_SUSPENDED / REPORT_STATUS_UPDATED
    private Long reportId;
    private Long profileId;
    private Long reporterId;
    private String category;
    private String status;
    private String message;
    private String createdAt;
}