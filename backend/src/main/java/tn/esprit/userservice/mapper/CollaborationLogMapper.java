package tn.esprit.userservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.userservice.dto.CollaborationLogRequestDto;
import tn.esprit.userservice.dto.CollaborationLogResponseDto;
import tn.esprit.userservice.entity.CollaborationLog;

@Component
public class CollaborationLogMapper {

    public CollaborationLog toEntity(CollaborationLogRequestDto dto) {
        if (dto == null) {
            return null;
        }

        tn.esprit.userservice.entity.User sender = new tn.esprit.userservice.entity.User();
        sender.setId(dto.getUserId());

        return CollaborationLog.builder()
                .sender(sender)
                .message(dto.getMessage())
                .build();
    }

    public CollaborationLogResponseDto toResponseDto(CollaborationLog log) {
        if (log == null) {
            return null;
        }

        return CollaborationLogResponseDto.builder()
                .id(log.getId())
                .agencyId(log.getAgency() != null ? log.getAgency().getId() : null)
                .userId(log.getSender() != null ? log.getSender().getId() : null)
                .message(log.getMessage())
                .attachmentUrl(null)
                .sentAt(log.getSentAt())
                .build();
    }
}
