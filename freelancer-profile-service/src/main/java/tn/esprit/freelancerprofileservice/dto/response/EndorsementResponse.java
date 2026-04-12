package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de réponse — endorsement d'un skill
 */
@Data
@Builder
public class EndorsementResponse {

    private Long id;
    private Long endorserId;
    private String comment;
    private LocalDateTime endorsedAt;
}