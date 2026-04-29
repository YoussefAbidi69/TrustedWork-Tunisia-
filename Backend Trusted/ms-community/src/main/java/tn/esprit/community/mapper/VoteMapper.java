package tn.esprit.community.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tn.esprit.community.dto.VoteDTO;
import tn.esprit.community.entity.Vote;

@Mapper(componentModel = "spring")
public interface VoteMapper {
    @Mapping(target = "postId", source = "post.id")
    VoteDTO toDto(Vote vote);

    @Mapping(target = "post", ignore = true)
    Vote toEntity(VoteDTO voteDTO);
}
