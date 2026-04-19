package tn.esprit.userservice.mapper;


import org.springframework.stereotype.Component;
import tn.esprit.userservice.dto.TaskCommentRequestDto;
import tn.esprit.userservice.dto.TaskCommentResponseDto;
import tn.esprit.userservice.entity.TaskComment;

@Component
public class TaskCommentMapper {

    public TaskComment toEntity(TaskCommentRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return TaskComment.builder()
                .userId(dto.getUserId())
                .content(dto.getContent())
                .build();
    }

    public TaskCommentResponseDto toResponseDto(TaskComment comment) {
        if (comment == null) {
            return null;
        }

        return TaskCommentResponseDto.builder()
                .id(comment.getId())
                .taskId(comment.getTask().getId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .commentedAt(comment.getCommentedAt())
                .build();
    }
}