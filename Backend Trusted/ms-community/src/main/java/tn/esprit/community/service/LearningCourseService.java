package tn.esprit.community.service;

import java.util.List;
import tn.esprit.community.dto.lms.BlockDTO;
import tn.esprit.community.dto.lms.CourseDTO;
import tn.esprit.community.dto.lms.SectionDTO;

public interface LearningCourseService {

    CourseDTO getCourse(Long id);

    List<CourseDTO> listCoursesByCommunity(Long communityId, boolean publishedOnly);

    CourseDTO createCourse(CourseDTO dto);

    CourseDTO updateCourse(Long id, CourseDTO dto);

    List<SectionDTO> listSections(Long courseId);

    SectionDTO createSection(Long courseId, SectionDTO dto);

    SectionDTO updateSection(Long sectionId, SectionDTO dto);

    void deleteSection(Long sectionId);

    List<BlockDTO> listBlocks(Long sectionId);

    BlockDTO createBlock(Long sectionId, BlockDTO dto);

    BlockDTO updateBlock(Long blockId, BlockDTO dto);

    void deleteBlock(Long blockId);
}
