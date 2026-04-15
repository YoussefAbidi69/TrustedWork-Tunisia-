package tn.esprit.community.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tn.esprit.community.dto.ReportDTO;
import tn.esprit.community.entity.Report;

@Mapper(componentModel = "spring")
public interface ReportMapper {
    @Mapping(target = "postId", source = "post.id")
    ReportDTO toDto(Report report);

    @Mapping(target = "post", ignore = true)
    @Mapping(target = "status", ignore = true)
    Report toEntity(ReportDTO reportDTO);
}
