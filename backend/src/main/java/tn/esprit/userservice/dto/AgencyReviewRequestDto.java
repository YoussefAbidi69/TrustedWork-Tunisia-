package tn.esprit.userservice.dto;


import lombok.*;
import tn.esprit.userservice.entity.ReviewTargetType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyReviewRequestDto {

    private Integer rating;
    private String comment;
    private ReviewTargetType targetType;
    private Long projectId;
}