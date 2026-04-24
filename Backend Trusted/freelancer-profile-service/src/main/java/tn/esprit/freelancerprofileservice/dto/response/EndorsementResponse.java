package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de réponse d'un endorsement.
 */
@Data
@Builder
public class EndorsementResponse {

    private Long id;
    private Long endorserId;
    private String comment;
    private LocalDateTime endorsedAt;
}