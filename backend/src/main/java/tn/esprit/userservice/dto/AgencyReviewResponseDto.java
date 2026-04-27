package tn.esprit.userservice.dto;


import lombok.*;
import tn.esprit.userservice.entity.ReviewTargetType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyReviewResponseDto {

    private Long id;
    private Long agencyId;
    private Long projectId;

    private Integer rating;
    private String comment;
    private ReviewTargetType targetType;

    private LocalDateTime reviewedAt;
}