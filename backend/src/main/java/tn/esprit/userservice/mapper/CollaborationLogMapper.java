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

        return CollaborationLog.builder()
                .userId(dto.getUserId())
                .message(dto.getMessage())
                .attachmentUrl(dto.getAttachmentUrl())
                .build();
    }

    public CollaborationLogResponseDto toResponseDto(CollaborationLog log) {
        if (log == null) {
            return null;
        }

        return CollaborationLogResponseDto.builder()
                .id(log.getId())
                .agencyId(log.getAgency().getId())
                .userId(log.getUserId())
                .message(log.getMessage())
                .attachmentUrl(log.getAttachmentUrl())
                .sentAt(log.getSentAt())
                .build();
    }
}
