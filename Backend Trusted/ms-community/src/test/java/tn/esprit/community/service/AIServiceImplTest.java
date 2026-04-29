package tn.esprit.community.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import tn.esprit.community.dto.ai.CourseOutlineResponse;
import tn.esprit.community.dto.ai.QuizQuestion;
import tn.esprit.community.service.impl.AIServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIServiceImplTest {

    @Test
    @DisplayName("shouldReturnEmptySummary_whenApiKeyMissing")
    void shouldReturnEmptySummary_whenApiKeyMissing() {
        AIServiceImpl service = new AIServiceImpl();
        ReflectionTestUtils.setField(service, "apiKey", "");

        String summary = service.summarizeLesson("content");

        assertThat(summary).isEqualTo("");
    }

    @Test
    @DisplayName("shouldParseCourseOutline_whenAiReturnsJson")
    void shouldParseCourseOutline_whenAiReturnsJson() {
        AIServiceImpl service = new AIServiceImpl();
        RestTemplate restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "apiUrl", "http://ai");
        ReflectionTestUtils.setField(service, "apiKey", "key");
        ReflectionTestUtils.setField(service, "model", "model");
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        String json = "{\"topic\":\"Java\",\"level\":\"Beginner\",\"sections\":[{\"title\":\"Intro\",\"lessons\":[\"L1\"]}]}";
        Map<String, Object> response = Map.of(
                "choices", List.of(Map.of("message", Map.of("content", json)))
        );
        when(restTemplate.postForObject(eq("http://ai"), any(), eq(Map.class))).thenReturn(response);

        CourseOutlineResponse outline = service.generateCourseOutline("Java", "Beginner");

        assertThat(outline.getTopic()).isEqualTo("Java");
        assertThat(outline.getSections()).hasSize(1);
    }

    @Test
    @DisplayName("shouldParseQuizQuestions_whenAiReturnsJson")
    void shouldParseQuizQuestions_whenAiReturnsJson() {
        AIServiceImpl service = new AIServiceImpl();
        RestTemplate restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "apiUrl", "http://ai");
        ReflectionTestUtils.setField(service, "apiKey", "key");
        ReflectionTestUtils.setField(service, "model", "model");
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        String json = "{\"questions\":[{\"question\":\"Q1\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctIndex\":1}]}";
        Map<String, Object> response = Map.of(
                "choices", List.of(Map.of("message", Map.of("content", json)))
        );
        when(restTemplate.postForObject(eq("http://ai"), any(), eq(Map.class))).thenReturn(response);

        List<QuizQuestion> quiz = service.generateQuiz("lesson");

        assertThat(quiz).hasSize(1);
        assertThat(quiz.get(0).getQuestion()).isEqualTo("Q1");
    }
}
