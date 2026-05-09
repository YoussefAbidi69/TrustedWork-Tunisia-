package tn.esprit.freelancerprofileservice.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message WebSocket envoyé en temps réel lors d'un nouvel avis client.
 * Publié sur : /topic/user/{userId}/notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewNotificationMessage {

    private String type;       // NEW_REVIEW
    private Long profileId;
    private Long clientId;
    private Integer rating;
    private Boolean flagged;
    private String message;    // Texte affiché dans la cloche
    private String createdAt;
}