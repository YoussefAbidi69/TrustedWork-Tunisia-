package tn.esprit.userservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.userservice.dto.AgencyReviewRequestDto;
import tn.esprit.userservice.dto.AgencyReviewResponseDto;
import tn.esprit.userservice.entity.AgencyReview;

@Component
public class AgencyReviewMapper {

    public AgencyReview toEntity(AgencyReviewRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return AgencyReview.builder()
                .rating(dto.getRating())
                .comment(dto.getComment())
                .targetType(dto.getTargetType())
                .projectId(dto.getProjectId())
                .build();
    }

    public AgencyReviewResponseDto toResponseDto(AgencyReview review) {
        if (review == null) {
            return null;
        }

        return AgencyReviewResponseDto.builder()
                .id(review.getId())
                .agencyId(review.getAgency().getId())
                .projectId(review.getProjectId())
                .rating(review.getRating())
                .comment(review.getComment())
                .targetType(review.getTargetType())
                .reviewedAt(review.getReviewedAt())
                .build();
    }
}