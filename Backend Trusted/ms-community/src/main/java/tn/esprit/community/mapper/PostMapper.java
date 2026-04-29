package tn.esprit.community.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tn.esprit.community.dto.PostDTO;
import tn.esprit.community.entity.Post;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = {"myVote", "upvoteCount", "downvoteCount"})
    @Mapping(target = "community", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "votes", ignore = true)
    @Mapping(target = "reports", ignore = true)
    Post toEntity(PostDTO dto);

    @Mapping(target = "communityId", source = "community.id")
    @Mapping(target = "myVote", ignore = true)
    PostDTO toDto(Post post);
}
