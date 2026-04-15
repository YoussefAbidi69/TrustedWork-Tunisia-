package tn.esprit.community.mapper;

import org.mapstruct.Mapper;
import tn.esprit.community.entity.Contribution;
import tn.esprit.community.dto.ContributionDTO;

@Mapper(componentModel = "spring")
public interface ContributionMapper {
    ContributionDTO toDto(Contribution contribution);
    Contribution toEntity(ContributionDTO contributionDTO);
}
