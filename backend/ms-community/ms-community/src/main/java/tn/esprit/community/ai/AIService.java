package tn.esprit.community.ai;

import java.util.List;
import tn.esprit.community.ai.dto.CourseOutlineRequest;
import tn.esprit.community.ai.dto.QuizRequest;
import tn.esprit.community.ai.dto.SummaryRequest;
import tn.esprit.community.ai.dto.TutorRequest;
import tn.esprit.community.ai.dto.CourseOutlineResponse;
import tn.esprit.community.ai.dto.QuizQuestion;

public interface AIService {
    CourseOutlineResponse generateCourseOutline(String topic, String level);
    List<QuizQuestion> generateQuiz(String lessonContent);
    String summarizeLesson(String lessonContent);
    String tutorAnswer(String courseContent, String question);
}
