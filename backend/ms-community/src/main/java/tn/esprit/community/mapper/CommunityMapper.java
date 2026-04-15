package tn.esprit.community.mapper;

import org.mapstruct.Mapper;
import tn.esprit.community.entity.Community;
import tn.esprit.community.dto.CommunityDTO;

@Mapper(componentModel = "spring")
public interface CommunityMapper {
    CommunityDTO toDto(Community community);
    Community toEntity(CommunityDTO communityDTO);
}
