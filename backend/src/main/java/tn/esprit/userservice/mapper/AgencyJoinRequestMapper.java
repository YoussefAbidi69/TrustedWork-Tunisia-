package tn.esprit.userservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.userservice.dto.AgencyJoinRequestResponseDto;
import tn.esprit.userservice.entity.AgencyJoinRequest;

@Component
public class AgencyJoinRequestMapper {

    public AgencyJoinRequestResponseDto toResponseDto(AgencyJoinRequest request) {
        if (request == null) return null;

        return AgencyJoinRequestResponseDto.builder()
                .id(request.getId())
                .agencyId(request.getAgency() != null ? request.getAgency().getId() : null)
                .agencyName(request.getAgency() != null ? request.getAgency().getName() : null)
                .requesterId(request.getRequester() != null ? request.getRequester().getId() : null)
                .requesterFirstName(request.getRequester() != null ? request.getRequester().getFirstName() : null)
                .requesterLastName(request.getRequester() != null ? request.getRequester().getLastName() : null)
                .requesterEmail(request.getRequester() != null ? request.getRequester().getEmail() : null)
                .requesterPhoto(request.getRequester() != null ? request.getRequester().getPhoto() : null)
                .requesterSkills(request.getRequester() != null ? request.getRequester().getSkills() : null)
                .requesterHeadline(request.getRequester() != null ? request.getRequester().getHeadline() : null)
                .status(request.getStatus())
                .message(request.getMessage())
                .requestedAt(request.getRequestedAt())
                .respondedAt(request.getRespondedAt())
                .build();
    }
}
