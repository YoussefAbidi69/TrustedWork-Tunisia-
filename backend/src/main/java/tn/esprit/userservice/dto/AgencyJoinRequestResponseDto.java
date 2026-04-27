package tn.esprit.userservice.dto;

import lombok.*;
import tn.esprit.userservice.entity.JoinRequestStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyJoinRequestResponseDto {
    private Long id;
    private Long agencyId;
    private String agencyName;
    private Long requesterId;
    private String requesterFirstName;
    private String requesterLastName;
    private String requesterEmail;
    private String requesterPhoto;
    private String requesterSkills;
    private String requesterHeadline;
    private JoinRequestStatus status;
    private String message;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
}
