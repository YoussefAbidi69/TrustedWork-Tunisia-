package tn.esprit.community.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.community.dto.request.CourseRequest;
import tn.esprit.community.dto.response.BlockResponse;
import tn.esprit.community.dto.response.CourseDownloadResponse;
import tn.esprit.community.dto.response.CourseResponse;
import tn.esprit.community.dto.response.SectionResponse;
import tn.esprit.community.entity.Block;
import tn.esprit.community.entity.Community;
import tn.esprit.community.entity.Course;
import tn.esprit.community.entity.Section;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.repository.BlockRepository;
import tn.esprit.community.repository.CommunityRepository;
import tn.esprit.community.repository.SectionRepository;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.service.CourseService;
import tn.esprit.community.exception.PlagiarismException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.esprit.community.service.DiscordNotificationService;

@Service
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CommunityRepository communityRepository;
    private final SectionRepository sectionRepository;
    private final BlockRepository blockRepository;
    private final WebClient aiClient;
    private final DiscordNotificationService discordNotificationService;
    private static final Logger logger = LoggerFactory.getLogger(CourseServiceImpl.class);
    private static final String COURSE_NOT_FOUND = "Course not found";

    public CourseServiceImpl(
            CourseRepository courseRepository,
            CommunityRepository communityRepository,
            SectionRepository sectionRepository,
            BlockRepository blockRepository,
            @Value("${app.course-quality.base-url:http://localhost:5000}") String aiBaseUrl,
            DiscordNotificationService discordNotificationService) {
        this.courseRepository = courseRepository;
        this.communityRepository = communityRepository;
        this.sectionRepository = sectionRepository;
        this.blockRepository = blockRepository;
        this.aiClient = WebClient.builder().baseUrl(aiBaseUrl).build();
        this.discordNotificationService = discordNotificationService;
    }

    @Override
    @Transactional
    public CourseResponse createCourse(CourseRequest courseRequest) {
        Community community = null;
        if (courseRequest.getCommunityId() != null) {
            community = communityRepository
                    .findById(courseRequest.getCommunityId())
                    .orElseThrow(() -> new LearningNotFoundException("Community not found"));
        }

        Course course = Course.builder()
                .title(courseRequest.getTitle())
                .description(courseRequest.getDescription())

                .authorId(courseRequest.getAuthorId())
                .published(Boolean.TRUE.equals(courseRequest.getPublished()))
                .community(community)
                .build();

        CourseResponse response = toCourseResponse(courseRepository.save(course));
        if (Boolean.TRUE.equals(course.isPublished())) {
            discordNotificationService.notifyCoursePublished(response);
        }
        return response;
    }

    @Override
    public CourseResponse getCourse(Long id) {
        Course course = courseRepository.findById(id).orElseThrow(() -> new LearningNotFoundException(COURSE_NOT_FOUND));
        return toCourseResponse(course);
    }

    @Override
    public List<CourseResponse> listCourses(Long communityId, Boolean publishedOnly) {
        boolean published = Boolean.TRUE.equals(publishedOnly);
        List<Course> courses;

        if (communityId != null && published) {
            courses = courseRepository.findByCommunityIdAndPublishedTrueOrderByTitleAsc(communityId);
        } else if (communityId != null) {
            courses = courseRepository.findByCommunityIdOrderByTitleAsc(communityId);
        } else {
            courses = courseRepository.findAll();
            if (published) {
                courses = courses.stream().filter(Course::isPublished).toList();
            }
        }

        return courses.stream().map(this::toCourseResponse).toList();
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest courseRequest) {
        Course course = courseRepository.findById(id).orElseThrow(() -> new LearningNotFoundException(COURSE_NOT_FOUND));

        updateBasicFields(course, courseRequest);
        boolean justPublished = handlePublishing(course, courseRequest, id);

        CourseResponse response = toCourseResponse(courseRepository.save(course));
        if (justPublished) {
            discordNotificationService.notifyCoursePublished(response);
        }
        return response;
    }

    private void updateBasicFields(Course course, CourseRequest courseRequest) {
        if (courseRequest.getTitle() != null) {
            course.setTitle(courseRequest.getTitle());
        }
        if (courseRequest.getDescription() != null) {
            course.setDescription(courseRequest.getDescription());
        }
        if (courseRequest.getAuthorId() != null) {
            course.setAuthorId(courseRequest.getAuthorId());
        }
        if (courseRequest.getCommunityId() != null) {
            Community community = communityRepository
                    .findById(courseRequest.getCommunityId())
                    .orElseThrow(() -> new LearningNotFoundException("Community not found"));
            course.setCommunity(community);
        }
    }

    private boolean handlePublishing(Course course, CourseRequest courseRequest, Long id) {
        if (courseRequest.getPublished() == null) {
            return false;
        }

        boolean justPublished = false;
        if (Boolean.TRUE.equals(courseRequest.getPublished()) && !course.isPublished()) {
            justPublished = true;
            checkPlagiarism(id);
        }

        course.setPublished(courseRequest.getPublished());

        if (Boolean.TRUE.equals(course.isPublished())) {
            updateAiIndex();
        }

        return justPublished;
    }

    private void checkPlagiarism(Long id) {
        CourseDownloadResponse fullCourseData = downloadCourse(id);
        try {
            Map<String, Object> response = aiClient.post()
                    .uri("/check_plagiarism")
                    .bodyValue(fullCourseData)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && Boolean.TRUE.equals(response.get("is_plagiarized"))) {
                double similarity = (double) response.get("max_similarity");
                throw new PlagiarismException("Cannot publish: This course matches an existing course by " + similarity + "%.");
            }
        } catch (PlagiarismException e) {
            throw e; // Rethrow custom exception
        } catch (Exception e) {
            // If AI server is down, we allow publishing or we can block it. Let's block it for safety or log it.
            logger.error("Failed to reach Plagiarism Checker API: {}", e.getMessage(), e);
        }
    }

    private void updateAiIndex() {
        try {
            aiClient.post()
                .uri("/update_index")
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                    v -> logger.info("AI Plagiarism index updated."),
                    err -> logger.error("Failed to update AI index: {}", err.getMessage(), err)
                );
        } catch (Exception e) {
            logger.error("Error calling update_index: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new LearningNotFoundException(COURSE_NOT_FOUND);
        }
        courseRepository.deleteById(id);
    }

    @Override
    public CourseDownloadResponse downloadCourse(Long courseId) {
        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(() -> new LearningNotFoundException(COURSE_NOT_FOUND));

        List<Section> sections = sectionRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        List<SectionResponse> sectionResponses = sections.stream().map(this::toSectionTree).toList();

        return CourseDownloadResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .authorId(course.getAuthorId())
                .communityId(course.getCommunity() != null ? course.getCommunity().getId() : null)
                .published(course.isPublished())
                .sections(sectionResponses)
                .build();
    }

    private SectionResponse toSectionTree(Section section) {
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

    private CourseResponse toCourseResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .authorId(course.getAuthorId())
                .communityId(course.getCommunity() != null ? course.getCommunity().getId() : null)
                .published(course.isPublished())
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
