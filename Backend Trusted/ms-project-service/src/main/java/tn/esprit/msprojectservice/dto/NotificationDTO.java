package tn.esprit.msprojectservice.dto;

import lombok.*;
import tn.esprit.msprojectservice.entities.Notification;
import tn.esprit.msprojectservice.entities.NotificationType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private Long id;
    private Long userId;
    private String title;
    private String message;
    private NotificationType type;
    private Long projectId;
    private Long taskId;
    private boolean read;
    private LocalDateTime createdAt;

    // --- Mapping Entity -> DTO ---
    public static NotificationDTO fromEntity(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .projectId(notification.getProjectId())
                .taskId(notification.getTaskId())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}