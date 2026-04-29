package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;
import tn.esprit.freelancerprofileservice.enums.ReviewStatus;

import java.time.LocalDateTime;

/**
 * DTO de réponse — avis client
 */
@Data
@Builder
public class ReviewResponse {

    private Long id;

    private Long clientId;

    private Integer rating;

    private String comment;


    private String freelancerReply;


    private Boolean flagged;


    private String flagReason;

    private ReviewStatus status;

    private LocalDateTime reviewedAt;


    private LocalDateTime updatedAt;
}