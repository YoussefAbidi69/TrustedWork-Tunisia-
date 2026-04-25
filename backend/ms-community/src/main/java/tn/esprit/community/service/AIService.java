package tn.esprit.community.service;

import java.util.List;
import tn.esprit.community.dto.ai.CourseOutlineResponse;
import tn.esprit.community.dto.ai.QuizQuestion;

public interface AIService {
    CourseOutlineResponse generateCourseOutline(String topic, String level);

    List<QuizQuestion> generateQuiz(String lessonContent);

    String summarizeLesson(String lessonContent);

    String tutorAnswer(String courseContent, String question);
}
