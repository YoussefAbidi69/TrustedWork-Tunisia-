package tn.esprit.community.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tn.esprit.community.dto.lms.BlockDTO;
import tn.esprit.community.dto.lms.CourseDTO;
import tn.esprit.community.dto.lms.SectionDTO;
import tn.esprit.community.dto.request.BlockRequest;
import tn.esprit.community.dto.request.CourseRequest;
import tn.esprit.community.dto.request.SectionRequest;
import tn.esprit.community.dto.response.BlockResponse;
import tn.esprit.community.dto.response.CourseResponse;
import tn.esprit.community.dto.response.SectionResponse;
import tn.esprit.community.service.BlockService;
import tn.esprit.community.service.CourseService;
import tn.esprit.community.service.LearningCourseService;
import tn.esprit.community.service.SectionService;

@Service
public class LearningCourseServiceImpl implements LearningCourseService {

    private final CourseService courseService;
    private final SectionService sectionService;
    private final BlockService blockService;

    public LearningCourseServiceImpl(
            CourseService courseService, SectionService sectionService, BlockService blockService) {
        this.courseService = courseService;
        this.sectionService = sectionService;
        this.blockService = blockService;
    }

    @Override
    public CourseDTO getCourse(Long id) {
        return toCourseDto(courseService.getCourse(id));
    }

    @Override
    public List<CourseDTO> listCoursesByCommunity(Long communityId, boolean publishedOnly) {
        return courseService.listCourses(communityId, publishedOnly).stream()
                .map(this::toCourseDto)
                .collect(Collectors.toList());
    }

    @Override
    public CourseDTO createCourse(CourseDTO dto) {
        CourseResponse response = courseService.createCourse(CourseRequest.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .authorId(dto.getAuthorId())
                .communityId(dto.getCommunityId())
                .published(dto.isPublished())
                .build());
        return toCourseDto(response);
    }

    @Override
    public CourseDTO updateCourse(Long id, CourseDTO dto) {
        CourseResponse response = courseService.updateCourse(id, CourseRequest.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .authorId(dto.getAuthorId())
                .communityId(dto.getCommunityId())
                .published(dto.isPublished())
                .build());
        return toCourseDto(response);
    }

    @Override
    public List<SectionDTO> listSections(Long courseId) {
        return sectionService.listSections(courseId).stream().map(this::toSectionDto).collect(Collectors.toList());
    }

    @Override
    public SectionDTO createSection(Long courseId, SectionDTO dto) {
        return toSectionDto(sectionService.createSection(courseId, SectionRequest.builder()
                .title(dto.getTitle())
                .orderIndex(dto.getOrderIndex())
                .build()));
    }

    @Override
    public SectionDTO updateSection(Long sectionId, SectionDTO dto) {
        return toSectionDto(sectionService.updateSection(sectionId, SectionRequest.builder()
                .title(dto.getTitle())
                .orderIndex(dto.getOrderIndex())
                .build()));
    }

    @Override
    public void deleteSection(Long sectionId) {
        sectionService.deleteSection(sectionId);
    }

    @Override
    public List<BlockDTO> listBlocks(Long sectionId) {
        return blockService.listBlocks(sectionId).stream().map(this::toBlockDto).collect(Collectors.toList());
    }

    @Override
    public BlockDTO createBlock(Long sectionId, BlockDTO dto) {
        return toBlockDto(blockService.createBlock(sectionId, BlockRequest.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .fileUrl(dto.getFileUrl())
                .orderIndex(dto.getOrderIndex())
                .type(dto.getType())
                .build()));
    }

    @Override
    public BlockDTO updateBlock(Long blockId, BlockDTO dto) {
        return toBlockDto(blockService.updateBlock(blockId, BlockRequest.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .fileUrl(dto.getFileUrl())
                .orderIndex(dto.getOrderIndex())
                .type(dto.getType())
                .build()));
    }

    @Override
    public void deleteBlock(Long blockId) {
        blockService.deleteBlock(blockId);
    }

    private CourseDTO toCourseDto(CourseResponse response) {
        return CourseDTO.builder()
                .id(response.getId())
                .title(response.getTitle())
                .description(response.getDescription())
                .authorId(response.getAuthorId())
                .communityId(response.getCommunityId())
                .published(response.isPublished())
                .build();
    }

    private SectionDTO toSectionDto(SectionResponse response) {
        return SectionDTO.builder()
                .id(response.getId())
                .courseId(response.getCourseId())
                .title(response.getTitle())
                .orderIndex(response.getOrderIndex())
                .build();
    }

    private BlockDTO toBlockDto(BlockResponse response) {
        return BlockDTO.builder()
                .id(response.getId())
                .sectionId(response.getSectionId())
                .title(response.getTitle())
                .content(response.getContent())
                .fileUrl(response.getFileUrl())
                .orderIndex(response.getOrderIndex())
                .type(response.getType())
                .build();
    }
}
