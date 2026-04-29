package tn.esprit.community.controller;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tn.esprit.community.dto.ai.CourseOutlineRequest;
import tn.esprit.community.dto.ai.CourseOutlineResponse;
import tn.esprit.community.dto.ai.QuizQuestion;
import tn.esprit.community.dto.ai.QuizRequest;
import tn.esprit.community.dto.ai.SummaryRequest;
import tn.esprit.community.dto.ai.TutorRequest;
import tn.esprit.community.service.AIService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIControllerTest {

    @Test
    @DisplayName("shouldDefaultToEmptyStrings_whenRequestIsNull")
    void shouldDefaultToEmptyStrings_whenRequestIsNull() {
        AIService aiService = mock(AIService.class);
        CourseOutlineResponse response = CourseOutlineResponse.builder().topic("").level("").sections(List.of()).build();
        when(aiService.generateCourseOutline("", "")).thenReturn(response);
        AIController controller = new AIController(aiService);

        CourseOutlineResponse result = controller.generateCourseOutline(null).getBody();

        assertThat(result).isNotNull();
        verify(aiService).generateCourseOutline("", "");
    }

    @Test
    @DisplayName("shouldCallAiService_whenQuizRequested")
    void shouldCallAiService_whenQuizRequested() {
        AIService aiService = mock(AIService.class);
        when(aiService.generateQuiz("lesson")).thenReturn(List.of(QuizQuestion.builder().question("Q").build()));
        AIController controller = new AIController(aiService);

        List<QuizQuestion> result = controller.generateQuiz(QuizRequest.builder().lessonContent("lesson").build()).getBody();

        assertThat(result).hasSize(1);
        verify(aiService).generateQuiz("lesson");
    }

    @Test
    @DisplayName("shouldCallAiService_whenSummarizeRequested")
    void shouldCallAiService_whenSummarizeRequested() {
        AIService aiService = mock(AIService.class);
        when(aiService.summarizeLesson("")).thenReturn("");
        AIController controller = new AIController(aiService);

        String summary = controller.summarizeLesson(new SummaryRequest()).getBody();

        assertThat(summary).isEqualTo("");
        verify(aiService).summarizeLesson("");
    }

    @Test
    @DisplayName("shouldCallAiService_whenTutorAnswerRequested")
    void shouldCallAiService_whenTutorAnswerRequested() {
        AIService aiService = mock(AIService.class);
        when(aiService.tutorAnswer("course", "question")).thenReturn("answer");
        AIController controller = new AIController(aiService);

        String answer = controller.tutorAnswer(TutorRequest.builder().courseContent("course").question("question").build()).getBody();

        assertThat(answer).isEqualTo("answer");
        verify(aiService).tutorAnswer("course", "question");
    }
}
