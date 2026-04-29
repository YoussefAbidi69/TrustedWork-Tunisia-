package tn.esprit.msprojectservice.dto;

import lombok.*;
import tn.esprit.msprojectservice.entities.Deliverable;
import tn.esprit.msprojectservice.entities.DeliverableStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliverableDTO {

    private Long id;
    private String title;
    private String description;
    private String fileUrl;
    private DeliverableStatus status;
    private Long taskId;
    private Long projectId;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String reviewComment;

    // --- Mapping Entity -> DTO ---
    public static DeliverableDTO fromEntity(Deliverable deliverable) {
        return DeliverableDTO.builder()
                .id(deliverable.getId())
                .title(deliverable.getTitle())
                .description(deliverable.getDescription())
                .fileUrl(deliverable.getFileUrl())
                .status(deliverable.getStatus())
                .taskId(deliverable.getTask() != null ? deliverable.getTask().getId() : null)
                .projectId(deliverable.getProject() != null ? deliverable.getProject().getId() : null)
                .submittedAt(deliverable.getSubmittedAt())
                .reviewedAt(deliverable.getReviewedAt())
                .reviewComment(deliverable.getReviewComment())
                .build();
    }

    // --- Mapping DTO -> Entity (sans Project et Task, settés dans le service) ---
    public static Deliverable toEntity(DeliverableDTO dto) {
        return Deliverable.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .fileUrl(dto.getFileUrl())
                .status(dto.getStatus())
                .reviewComment(dto.getReviewComment())
                .build();
    }
}