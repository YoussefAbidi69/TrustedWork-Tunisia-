package tn.esprit.community.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.community.dto.request.SectionRequest;
import tn.esprit.community.dto.response.BlockResponse;
import tn.esprit.community.dto.response.SectionResponse;
import tn.esprit.community.entity.Block;
import tn.esprit.community.entity.Course;
import tn.esprit.community.entity.Section;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.repository.BlockRepository;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.repository.SectionRepository;
import tn.esprit.community.service.SectionService;

@Service
@Transactional(readOnly = true)
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final BlockRepository blockRepository;
    private static final String SECTION_NOT_FOUND = "Section not found";

    public SectionServiceImpl(
            SectionRepository sectionRepository, CourseRepository courseRepository, BlockRepository blockRepository) {
        this.sectionRepository = sectionRepository;
        this.courseRepository = courseRepository;
        this.blockRepository = blockRepository;
    }

    @Override
    @Transactional
    public SectionResponse createSection(Long courseId, SectionRequest sectionRequest) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new LearningNotFoundException("Course not found"));

        int orderIndex = sectionRequest.getOrderIndex() != null
                ? sectionRequest.getOrderIndex()
                : nextSectionOrder(courseId);

        Section section = Section.builder()
                .course(course)
                .title(sectionRequest.getTitle())
                .orderIndex(orderIndex)
                .build();

        return toSectionResponse(sectionRepository.save(section));
    }

    @Override
    @Transactional
    public SectionResponse updateSection(Long sectionId, SectionRequest sectionRequest) {
        Section section = sectionRepository
                .findById(sectionId)
                .orElseThrow(() -> new LearningNotFoundException(SECTION_NOT_FOUND));

        if (sectionRequest.getTitle() != null) {
            section.setTitle(sectionRequest.getTitle());
        }
        if (sectionRequest.getOrderIndex() != null) {
            section.setOrderIndex(sectionRequest.getOrderIndex());
        }

        return toSectionResponse(sectionRepository.save(section));
    }

    @Override
    public List<SectionResponse> listSections(Long courseId) {
        return sectionRepository.findByCourseIdOrderByOrderIndexAsc(courseId).stream()
                .map(this::toSectionResponse)
                .toList();
    }

    @Override
    public SectionResponse getSection(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .map(this::toSectionResponse)
                .orElseThrow(() -> new LearningNotFoundException(SECTION_NOT_FOUND));
    }

    @Override
    @Transactional
    public void deleteSection(Long sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new LearningNotFoundException(SECTION_NOT_FOUND);
        }
        sectionRepository.deleteById(sectionId);
    }

    private int nextSectionOrder(Long courseId) {
        return sectionRepository.findByCourseIdOrderByOrderIndexAsc(courseId).stream()
                .mapToInt(Section::getOrderIndex)
                .max()
                .orElse(-1) + 1;
    }

    private SectionResponse toSectionResponse(Section section) {
        List<BlockResponse> blocks = blockRepository.findBySectionIdOrderByOrderIndexAsc(section.getId()).stream()
                .map(this::toBlockResponse)
                .toList();

        return SectionResponse.builder()
                .id(section.getId())
                .courseId(section.getCourse() != null ? section.getCourse().getId() : null)
                .title(section.getTitle())
                .orderIndex(section.getOrderIndex())
                .blocks(blocks)
                .build();
    }

    private BlockResponse toBlockResponse(Block block) {
        return BlockResponse.builder()
                .id(block.getId())
                .sectionId(block.getSection() != null ? block.getSection().getId() : null)
                .title(block.getTitle())
                .content(block.getContent())
                .fileUrl(block.getFileUrl())
                .orderIndex(block.getOrderIndex())
                .type(block.getType())
                .build();
    }
}
