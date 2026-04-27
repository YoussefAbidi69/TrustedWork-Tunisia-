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
public class TeamProjectUpdateDto {
    private String name;
    private String description;
    private Float budget;
    private ProjectStatus status;
    private ProjectPriority priority;
    private Integer progress;
    private Boolean active;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}