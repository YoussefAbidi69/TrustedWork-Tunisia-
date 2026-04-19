package tn.esprit.userservice.dto;

import lombok.*;
import tn.esprit.userservice.entity.MemberRole;
import tn.esprit.userservice.entity.MemberStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyMemberRequestDto {
    private Long userId;
    private MemberRole role;
    private Float workloadScore;
    private MemberStatus status; // Replaced Boolean active
    private String skills;
}