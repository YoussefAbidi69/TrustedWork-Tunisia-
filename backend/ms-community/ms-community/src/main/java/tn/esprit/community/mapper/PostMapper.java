package tn.esprit.community.mapper;

import org.mapstruct.Mapper;
import tn.esprit.community.dto.PostDTO;
import tn.esprit.community.post.Post;

@Mapper(componentModel = "spring")
public interface PostMapper {
    PostDTO toDto(Post post);
    Post toEntity(PostDTO dto);
}
