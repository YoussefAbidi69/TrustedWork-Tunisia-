package tn.esprit.userservice.dto;

import lombok.*;
import tn.esprit.userservice.entity.MemberRole;
import tn.esprit.userservice.entity.MemberStatus;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyMemberResponseDto {
    private Long id;
    private Long userId; // The ID of the User entity
    private String firstName;
    private String lastName;
    private String email;
    private String photo;
    private String userSkills;
    private MemberRole role;
    private Float workloadScore;
    private MemberStatus status; // Replaced Boolean active
    private String skills;
    private LocalDateTime joinedAt;
}
