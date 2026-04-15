package tn.esprit.community.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import tn.esprit.community.dto.ai.CourseOutlineResponse;
import tn.esprit.community.dto.ai.QuizQuestion;
import tn.esprit.community.dto.ai.SectionOutline;
import tn.esprit.community.service.AIService;

@Service
public class AIServiceImpl implements AIService {
    @Override
    public CourseOutlineResponse generateCourseOutline(String topic, String level) {
        return CourseOutlineResponse.builder()
                .topic(topic)
                .level(level)
                .sections(List.of(
                        SectionOutline.builder().title("Introduction").lessons(List.of("Lesson 1", "Lesson 2", "Lesson 3")).build(),
                        SectionOutline.builder().title("Advanced Concepts").lessons(List.of("Lesson 1", "Lesson 2", "Lesson 3")).build()))
                .build();
    }

    @Override
    public List<QuizQuestion> generateQuiz(String lessonContent) {
        return List.of(
                QuizQuestion.builder().question("Question 1").options(List.of("A", "B", "C")).correctIndex(0).build(),
                QuizQuestion.builder().question("Question 2").options(List.of("A", "B", "C")).correctIndex(1).build(),
                QuizQuestion.builder().question("Question 3").options(List.of("A", "B", "C")).correctIndex(2).build());
    }

    @Override
    public String summarizeLesson(String lessonContent) {
        return "Summary: [placeholder bullet points for the provided content]";
    }

    @Override
    public String tutorAnswer(String courseContent, String question) {
        return "Based on the course content: [placeholder answer strictly derived from content]";
    }
}
