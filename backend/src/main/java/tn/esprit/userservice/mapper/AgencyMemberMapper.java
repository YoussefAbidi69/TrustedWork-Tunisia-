package tn.esprit.userservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.userservice.dto.AgencyMemberRequestDto;
import tn.esprit.userservice.dto.AgencyMemberResponseDto;
import tn.esprit.userservice.dto.AgencyMemberUpdateDto;
import tn.esprit.userservice.entity.AgencyMember;

@Component
public class AgencyMemberMapper {

    public AgencyMember toEntity(AgencyMemberRequestDto dto) {
        if (dto == null) {
            return null;
        }

        // Note: The User and Agency entities must be linked in the service layer
        return AgencyMember.builder()
                .role(dto.getRole())
                .workloadScore(dto.getWorkloadScore())
                .status(dto.getStatus())
                .skills(dto.getSkills())
                .build();
    }

    public AgencyMemberResponseDto toResponseDto(AgencyMember member) {
        if (member == null) {
            return null;
        }

        return AgencyMemberResponseDto.builder()
                .id(member.getId())
                .userId(member.getUser() != null ? member.getUser().getId() : null)
                .firstName(member.getUser() != null ? member.getUser().getFirstName() : null)
                .lastName(member.getUser() != null ? member.getUser().getLastName() : null)
                .email(member.getUser() != null ? member.getUser().getEmail() : null)
                .photo(member.getUser() != null ? member.getUser().getPhoto() : null)
                .userSkills(member.getUser() != null ? member.getUser().getSkills() : null)
                .role(member.getRole())
                .workloadScore(member.getWorkloadScore())
                .status(member.getStatus())
                .skills(member.getSkills())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    public void updateEntityFromDto(AgencyMemberUpdateDto dto, AgencyMember member) {
        if (dto == null || member == null) {
            return;
        }

        if (dto.getRole() != null) {
            member.setRole(dto.getRole());
        }

        if (dto.getWorkloadScore() != null) {
            member.setWorkloadScore(dto.getWorkloadScore());
        }

        if (dto.getStatus() != null) {
            member.setStatus(dto.getStatus());
        }

        if (dto.getSkills() != null) {
            member.setSkills(dto.getSkills());
        }
    }
}