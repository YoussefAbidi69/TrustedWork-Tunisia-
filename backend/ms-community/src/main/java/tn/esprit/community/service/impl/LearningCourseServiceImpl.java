package tn.esprit.community.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.community.dto.lms.CourseDTO;
import tn.esprit.community.dto.lms.LessonDTO;
import tn.esprit.community.dto.lms.SectionDTO;
import tn.esprit.community.entity.Course;
import tn.esprit.community.entity.Lesson;
import tn.esprit.community.entity.Section;
import tn.esprit.community.entity.Enum.LessonType;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.repository.CommunityRepository;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.repository.LessonRepository;
import tn.esprit.community.repository.SectionRepository;
import tn.esprit.community.service.LearningCourseService;

@Service
@Transactional(readOnly = true)
public class LearningCourseServiceImpl implements LearningCourseService {

    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final CommunityRepository communityRepository;

    public LearningCourseServiceImpl(
            CourseRepository courseRepository,
            SectionRepository sectionRepository,
            LessonRepository lessonRepository,
            CommunityRepository communityRepository) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.communityRepository = communityRepository;
    }

    @Override
    public CourseDTO getCourse(Long id) {
        return toCourseDto(courseRepository.findById(id).orElseThrow(() -> new LearningNotFoundException("Course not found")));
    }

    @Override
    public List<CourseDTO> listCoursesByCommunity(Long communityId, boolean publishedOnly) {
        List<Course> list =
                publishedOnly
                        ? courseRepository.findByCommunity_IdAndPublishedTrueOrderByTitleAsc(communityId)
                        : courseRepository.findByCommunity_IdOrderByTitleAsc(communityId);
        return list.stream().map(this::toCourseDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CourseDTO createCourse(CourseDTO dto) {
        if (dto.getCommunityId() == null) {
            throw new LearningNotFoundException("communityId is required");
        }
        var community = communityRepository
                .findById(dto.getCommunityId())
                .orElseThrow(() -> new LearningNotFoundException("Community not found"));
        Course course = Course.builder()
                .title(dto.getTitle())
                .description(dto.getDescription() != null ? dto.getDescription() : "")
                .authorId(dto.getAuthorId())
                .published(dto.isPublished())
                .community(community)
                .build();
        return toCourseDto(courseRepository.save(course));
    }

    @Override
    @Transactional
    public CourseDTO updateCourse(Long id, CourseDTO dto) {
        Course course = courseRepository.findById(id).orElseThrow(() -> new LearningNotFoundException("Course not found"));
        if (dto.getTitle() != null) {
            course.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            course.setDescription(dto.getDescription());
        }
        course.setPublished(dto.isPublished());
        if (dto.getCommunityId() != null) {
            var community = communityRepository
                    .findById(dto.getCommunityId())
                    .orElseThrow(() -> new LearningNotFoundException("Community not found"));
            course.setCommunity(community);
        }
        return toCourseDto(courseRepository.save(course));
    }

    @Override
    public List<SectionDTO> listSections(Long courseId) {
        ensureCourse(courseId);
        return sectionRepository.findByCourse_IdOrderByOrderIndexAsc(courseId).stream()
                .map(this::toSectionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SectionDTO createSection(Long courseId, SectionDTO dto) {
        Course course = ensureCourse(courseId);
        int order = dto.getOrderIndex() != null ? dto.getOrderIndex() : nextSectionOrder(courseId);
        Section section = Section.builder()
                .course(course)
                .title(dto.getTitle() != null ? dto.getTitle() : "Section")
                .orderIndex(order)
                .build();
        return toSectionDto(sectionRepository.save(section));
    }

    @Override
    @Transactional
    public SectionDTO updateSection(Long sectionId, SectionDTO dto) {
        Section section =
                sectionRepository.findById(sectionId).orElseThrow(() -> new LearningNotFoundException("Section not found"));
        if (dto.getTitle() != null) {
            section.setTitle(dto.getTitle());
        }
        if (dto.getOrderIndex() != null) {
            section.setOrderIndex(dto.getOrderIndex());
        }
        return toSectionDto(sectionRepository.save(section));
    }

    @Override
    @Transactional
    public void deleteSection(Long sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new LearningNotFoundException("Section not found");
        }
        sectionRepository.deleteById(sectionId);
    }

    @Override
    public List<LessonDTO> listLessons(Long sectionId) {
        ensureSection(sectionId);
        return lessonRepository.findBySection_IdOrderByOrderIndexAsc(sectionId).stream()
                .map(this::toLessonDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LessonDTO createLesson(Long sectionId, LessonDTO dto) {
        Section section = ensureSection(sectionId);
        int order = dto.getOrderIndex() != null ? dto.getOrderIndex() : nextLessonOrder(sectionId);
        LessonType type = dto.getType() != null ? dto.getType() : LessonType.TEXT;
        Lesson lesson = Lesson.builder()
                .section(section)
                .title(dto.getTitle() != null ? dto.getTitle() : "Lesson")
                .content(dto.getContent() != null ? dto.getContent() : "")
                .type(type)
                .videoUrl(nullToEmpty(dto.getVideoUrl()))
                .pdfUrl(nullToEmpty(dto.getPdfUrl()))
                .orderIndex(order)
                .build();
        return toLessonDto(lessonRepository.save(lesson));
    }

    @Override
    @Transactional
    public LessonDTO updateLesson(Long lessonId, LessonDTO dto) {
        Lesson lesson =
                lessonRepository.findById(lessonId).orElseThrow(() -> new LearningNotFoundException("Lesson not found"));
        if (dto.getTitle() != null) {
            lesson.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            lesson.setContent(dto.getContent());
        }
        if (dto.getType() != null) {
            lesson.setType(dto.getType());
        }
        if (dto.getVideoUrl() != null) {
            lesson.setVideoUrl(dto.getVideoUrl());
        }
        if (dto.getPdfUrl() != null) {
            lesson.setPdfUrl(dto.getPdfUrl());
        }
        if (dto.getOrderIndex() != null) {
            lesson.setOrderIndex(dto.getOrderIndex());
        }
        return toLessonDto(lessonRepository.save(lesson));
    }

    @Override
    @Transactional
    public void deleteLesson(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new LearningNotFoundException("Lesson not found");
        }
        lessonRepository.deleteById(lessonId);
    }

    private Course ensureCourse(Long courseId) {
        return courseRepository.findById(courseId).orElseThrow(() -> new LearningNotFoundException("Course not found"));
    }

    private Section ensureSection(Long sectionId) {
        return sectionRepository.findById(sectionId).orElseThrow(() -> new LearningNotFoundException("Section not found"));
    }

    private int nextSectionOrder(Long courseId) {
        return sectionRepository.findByCourse_IdOrderByOrderIndexAsc(courseId).stream()
                .mapToInt(Section::getOrderIndex)
                .max()
                .orElse(-1)
                + 1;
    }

    private int nextLessonOrder(Long sectionId) {
        return lessonRepository.findBySection_IdOrderByOrderIndexAsc(sectionId).stream()
                .mapToInt(Lesson::getOrderIndex)
                .max()
                .orElse(-1)
                + 1;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private CourseDTO toCourseDto(Course c) {
        Long communityId = c.getCommunity() != null ? c.getCommunity().getId() : null;
        return CourseDTO.builder()
                .id(c.getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .authorId(c.getAuthorId())
                .communityId(communityId)
                .published(c.isPublished())
                .build();
    }

    private SectionDTO toSectionDto(Section s) {
        return SectionDTO.builder()
                .id(s.getId())
                .courseId(s.getCourse() != null ? s.getCourse().getId() : null)
                .title(s.getTitle())
                .orderIndex(s.getOrderIndex())
                .build();
    }

    private LessonDTO toLessonDto(Lesson l) {
        return LessonDTO.builder()
                .id(l.getId())
                .sectionId(l.getSection() != null ? l.getSection().getId() : null)
                .title(l.getTitle())
                .content(l.getContent())
                .type(l.getType())
                .videoUrl(l.getVideoUrl())
                .pdfUrl(l.getPdfUrl())
                .orderIndex(l.getOrderIndex())
                .build();
    }
}
