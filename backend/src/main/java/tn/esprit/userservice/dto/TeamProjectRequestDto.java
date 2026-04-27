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
public class TeamProjectRequestDto {
    private String name;
    private String description;
    private Float budget;
    private ProjectStatus status;
    private ProjectPriority priority;
    private Integer progress;
    private Boolean active;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long creatorMemberId; // Added for project attribution
    private java.util.List<Long> assignedMembers; // List of AgencyMember IDs
}