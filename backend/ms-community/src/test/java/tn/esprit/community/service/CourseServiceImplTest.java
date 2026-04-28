package tn.esprit.community.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tn.esprit.community.dto.request.CourseRequest;
import tn.esprit.community.dto.response.CourseResponse;
import tn.esprit.community.entity.Community;
import tn.esprit.community.entity.Course;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.repository.BlockRepository;
import tn.esprit.community.repository.CommunityRepository;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.repository.SectionRepository;
import tn.esprit.community.service.impl.CourseServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CourseServiceImpl.
 * The internal AI WebClient is replaced with a Mockito mock so the publish and
 * plagiarism paths stay fully isolated from real HTTP calls.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CommunityRepository communityRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private BlockRepository blockRepository;
    @Mock private DiscordNotificationService discordNotificationService;
    @Mock private WebClient aiClient;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec requestBodySpec;
    @SuppressWarnings("rawtypes")
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private CourseServiceImpl courseService;

    private Course buildCourse(Long id, String title, boolean published) {
        return Course.builder()
                .id(id)
                .title(title)
                .description("desc")
                .authorId(1L)
                .published(published)
                .build();
    }

    @BeforeEach
    void setUp() {
        courseService = new CourseServiceImpl(
                courseRepository,
                communityRepository,
                sectionRepository,
                blockRepository,
                "http://localhost:9999",
                discordNotificationService
        );
        ReflectionTestUtils.setField(courseService, "aiClient", aiClient);

        lenient().when(aiClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());
        lenient().when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("shouldCreateCourse_whenNoCommunityProvided")
    void shouldCreateCourse_whenNoCommunityProvided() {
        CourseRequest request = CourseRequest.builder()
                .title("Java 101")
                .description("Intro")
                .authorId(42L)
                .build();

        Course savedCourse = buildCourse(1L, "Java 101", false);
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);

        CourseResponse response = courseService.createCourse(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Java 101");
        assertThat(response.isPublished()).isFalse();
        verify(courseRepository).save(any(Course.class));
        verify(discordNotificationService, never()).notifyCoursePublished(any());
    }

    @Test
    @DisplayName("shouldCreateCourse_andNotifyDiscord_whenPublishedIsTrue")
    void shouldCreateCourse_andNotifyDiscord_whenPublishedIsTrue() {
        CourseRequest request = CourseRequest.builder()
                .title("Spring Boot")
                .authorId(10L)
                .published(true)
                .build();

        Course savedCourse = buildCourse(2L, "Spring Boot", true);
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);

        CourseResponse response = courseService.createCourse(request);

        assertThat(response.isPublished()).isTrue();
        verify(discordNotificationService).notifyCoursePublished(any());
    }

    @Test
    @DisplayName("shouldCreateCourse_withCommunity_whenCommunityIdProvided")
    void shouldCreateCourse_withCommunity_whenCommunityIdProvided() {
        Community community = Community.builder().id(5L).name("ESPRIT").build();
        CourseRequest request = CourseRequest.builder()
                .title("Algorithms")
                .communityId(5L)
                .build();

        when(communityRepository.findById(5L)).thenReturn(Optional.of(community));
        Course savedCourse = Course.builder().id(3L).title("Algorithms").community(community).build();
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);

        CourseResponse response = courseService.createCourse(request);

        assertThat(response.getCommunityId()).isEqualTo(5L);
        verify(communityRepository).findById(5L);
    }

    @Test
    @DisplayName("shouldThrowLearningNotFoundException_whenCommunityNotFoundDuringCreate")
    void shouldThrowLearningNotFoundException_whenCommunityNotFoundDuringCreate() {
        CourseRequest request = CourseRequest.builder()
                .title("AI Basics")
                .communityId(99L)
                .build();

        when(communityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.createCourse(request))
                .isInstanceOf(LearningNotFoundException.class)
                .hasMessageContaining("Community not found");
    }

    @Test
    @DisplayName("shouldReturnCourseResponse_whenCourseExists")
    void shouldReturnCourseResponse_whenCourseExists() {
        Course course = buildCourse(10L, "DevOps", false);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        CourseResponse response = courseService.getCourse(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("DevOps");
    }

    @Test
    @DisplayName("shouldThrowLearningNotFoundException_whenCourseNotFoundOnGet")
    void shouldThrowLearningNotFoundException_whenCourseNotFoundOnGet() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourse(99L))
                .isInstanceOf(LearningNotFoundException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    @DisplayName("shouldReturnPublishedCoursesByCommunity_whenCommunityIdAndPublishedOnlyAreProvided")
    void shouldReturnPublishedCoursesByCommunity_whenCommunityIdAndPublishedOnlyAreProvided() {
        Course c = buildCourse(1L, "Spring", true);
        when(courseRepository.findByCommunity_IdAndPublishedTrueOrderByTitleAsc(3L))
                .thenReturn(List.of(c));

        List<CourseResponse> result = courseService.listCourses(3L, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Spring");
    }

    @Test
    @DisplayName("shouldReturnAllCoursesByCommunity_whenCommunityIdProvidedWithoutPublishedFilter")
    void shouldReturnAllCoursesByCommunity_whenCommunityIdProvidedWithoutPublishedFilter() {
        when(courseRepository.findByCommunity_IdOrderByTitleAsc(3L))
                .thenReturn(List.of(buildCourse(1L, "A", false), buildCourse(2L, "B", true)));

        List<CourseResponse> result = courseService.listCourses(3L, false);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("shouldReturnOnlyPublishedCourses_whenNoCommunityAndPublishedOnlyTrue")
    void shouldReturnOnlyPublishedCourses_whenNoCommunityAndPublishedOnlyTrue() {
        when(courseRepository.findAll()).thenReturn(
                List.of(buildCourse(1L, "Draft", false), buildCourse(2L, "Published", true))
        );

        List<CourseResponse> result = courseService.listCourses(null, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Published");
    }

    @Test
    @DisplayName("shouldReturnAllCourses_whenNoCommunityAndNoPublishedFilter")
    void shouldReturnAllCourses_whenNoCommunityAndNoPublishedFilter() {
        when(courseRepository.findAll()).thenReturn(
                List.of(buildCourse(1L, "A", false), buildCourse(2L, "B", true))
        );

        List<CourseResponse> result = courseService.listCourses(null, false);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("shouldUpdateCourseTitle_whenTitleProvidedInRequest")
    void shouldUpdateCourseTitle_whenTitleProvidedInRequest() {
        Course existing = buildCourse(5L, "Old Title", false);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(existing));

        Course updated = buildCourse(5L, "New Title", false);
        when(courseRepository.save(any(Course.class))).thenReturn(updated);

        CourseRequest request = CourseRequest.builder().title("New Title").build();
        CourseResponse response = courseService.updateCourse(5L, request);

        assertThat(response.getTitle()).isEqualTo("New Title");
        verify(courseRepository).save(existing);
    }

    @Test
    @DisplayName("shouldThrowLearningNotFoundException_whenCourseNotFoundOnUpdate")
    void shouldThrowLearningNotFoundException_whenCourseNotFoundOnUpdate() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        CourseRequest request = CourseRequest.builder().title("X").build();

        assertThatThrownBy(() -> courseService.updateCourse(99L, request))
                .isInstanceOf(LearningNotFoundException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    @DisplayName("shouldNotNotifyDiscord_whenCourseWasAlreadyPublishedBeforeUpdate")
    void shouldNotNotifyDiscord_whenCourseWasAlreadyPublishedBeforeUpdate() {
        Course existing = buildCourse(5L, "Course", true);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any())).thenReturn(existing);

        CourseRequest request = CourseRequest.builder().published(true).build();
        courseService.updateCourse(5L, request);

        verify(discordNotificationService, never()).notifyCoursePublished(any());
    }

    @Test
    @DisplayName("shouldCallPlagiarismServiceAndNotifyDiscord_whenPublishingDraftCourse")
    void shouldCallPlagiarismServiceAndNotifyDiscord_whenPublishingDraftCourse() {
        Course existing = buildCourse(8L, "Draft course", false);
        when(courseRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(sectionRepository.findByCourse_IdOrderByOrderIndexAsc(8L)).thenReturn(List.of());
        when(responseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(Map.of("is_plagiarized", false)));
        when(responseSpec.bodyToMono(eq(Void.class))).thenReturn(Mono.empty());
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseResponse response = courseService.updateCourse(8L, CourseRequest.builder().published(true).build());

        assertThat(response.isPublished()).isTrue();
        verify(aiClient, times(2)).post();
        verify(requestBodyUriSpec).uri("/check_plagiarism");
        verify(requestBodyUriSpec).uri("/update_index");
        verify(discordNotificationService).notifyCoursePublished(any());
    }

    @Test
    @DisplayName("shouldRejectPublishing_whenPlagiarismServiceFlagsCourse")
    void shouldRejectPublishing_whenPlagiarismServiceFlagsCourse() {
        Course existing = buildCourse(9L, "Draft course", false);
        when(courseRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(sectionRepository.findByCourse_IdOrderByOrderIndexAsc(9L)).thenReturn(List.of());
        when(responseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(Map.of(
                "is_plagiarized", true,
                "max_similarity", 87.5
        )));

        assertThatThrownBy(() -> courseService.updateCourse(9L, CourseRequest.builder().published(true).build()))
                .isInstanceOf(tn.esprit.community.exception.PlagiarismException.class)
                .hasMessageContaining("87.5%");

        verify(discordNotificationService, never()).notifyCoursePublished(any());
    }

    @Test
    @DisplayName("shouldDeleteCourse_whenCourseExists")
    void shouldDeleteCourse_whenCourseExists() {
        when(courseRepository.existsById(7L)).thenReturn(true);

        courseService.deleteCourse(7L);

        verify(courseRepository).deleteById(7L);
    }

    @Test
    @DisplayName("shouldThrowLearningNotFoundException_whenCourseNotFoundOnDelete")
    void shouldThrowLearningNotFoundException_whenCourseNotFoundOnDelete() {
        when(courseRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> courseService.deleteCourse(99L))
                .isInstanceOf(LearningNotFoundException.class)
                .hasMessageContaining("Course not found");

        verify(courseRepository, never()).deleteById(any());
    }
}
