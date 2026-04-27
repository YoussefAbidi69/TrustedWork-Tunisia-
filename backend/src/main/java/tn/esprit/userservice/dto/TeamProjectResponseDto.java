package tn.esprit.userservice.dto;


import lombok.*;
import tn.esprit.userservice.entity.ProjectPriority;
import tn.esprit.userservice.entity.ProjectStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamProjectResponseDto {
    private Long id;
    private String name;
    private String description;
    private Float budget;
    private ProjectStatus status;
    private ProjectPriority priority;
    private Integer progress;
    private Boolean active;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long creatorMemberId; // Link to the AgencyMember (Lead) who created it
    private Long agencyId;
    private java.util.List<AssignedMemberDto> assignedMembers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignedMemberDto {
        private Long memberId;
        private Long userId;
        private String firstName;
        private String lastName;
        private String photo;
    }
}
