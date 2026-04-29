package tn.esprit.community.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tn.esprit.community.entity.Comment;
import tn.esprit.community.dto.CommentDTO;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    @Mapping(target = "postId", source = "post.id")
    CommentDTO toDto(Comment comment);

    @Mapping(target = "post", ignore = true)
    Comment toEntity(CommentDTO commentDTO);
}
