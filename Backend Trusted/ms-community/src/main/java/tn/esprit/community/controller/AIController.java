package tn.esprit.community.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.ai.CourseOutlineRequest;
import tn.esprit.community.dto.ai.CourseOutlineResponse;
import tn.esprit.community.dto.ai.QuizQuestion;
import tn.esprit.community.dto.ai.QuizRequest;
import tn.esprit.community.dto.ai.SummaryRequest;
import tn.esprit.community.dto.ai.TutorRequest;
import tn.esprit.community.service.AIService;

@RestController
@RequestMapping("/api/ai")
public class AIController {
    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/course-outline")
    public ResponseEntity<CourseOutlineResponse> generateCourseOutline(@RequestBody CourseOutlineRequest request) {
        String topic = request != null && request.getTopic() != null ? request.getTopic() : "";
        String level = request != null && request.getLevel() != null ? request.getLevel() : "";
        CourseOutlineResponse response = aiService.generateCourseOutline(topic, level);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/quiz")
    public ResponseEntity<List<QuizQuestion>> generateQuiz(@RequestBody QuizRequest request) {
        String lessonContent = request != null && request.getLessonContent() != null ? request.getLessonContent() : "";
        List<QuizQuestion> quiz = aiService.generateQuiz(lessonContent);
        return new ResponseEntity<>(quiz, HttpStatus.OK);
    }

    @PostMapping("/summarize")
    public ResponseEntity<String> summarizeLesson(@RequestBody SummaryRequest request) {
        String lessonContent = request != null && request.getLessonContent() != null ? request.getLessonContent() : "";
        String summary = aiService.summarizeLesson(lessonContent);
        return new ResponseEntity<>(summary, HttpStatus.OK);
    }

    @PostMapping("/tutor-answer")
    public ResponseEntity<String> tutorAnswer(@RequestBody TutorRequest request) {
        String courseContent = request != null && request.getCourseContent() != null ? request.getCourseContent() : "";
        String question = request != null && request.getQuestion() != null ? request.getQuestion() : "";
        String answer = aiService.tutorAnswer(courseContent, question);
        return new ResponseEntity<>(answer, HttpStatus.OK);
    }
}
