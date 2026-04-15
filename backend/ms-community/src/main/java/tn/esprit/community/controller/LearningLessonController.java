package tn.esprit.community.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.lms.LessonDTO;
import tn.esprit.community.service.LearningCourseService;

@RestController
public class LearningLessonController {

    private final LearningCourseService learningCourseService;

    public LearningLessonController(LearningCourseService learningCourseService) {
        this.learningCourseService = learningCourseService;
    }

    @GetMapping("/api/sections/{sectionId}/lessons")
    public ResponseEntity<List<LessonDTO>> listLessons(@PathVariable Long sectionId) {
        return ResponseEntity.ok(learningCourseService.listLessons(sectionId));
    }

    @PostMapping("/api/sections/{sectionId}/lessons")
    public ResponseEntity<LessonDTO> createLesson(@PathVariable Long sectionId, @RequestBody LessonDTO dto) {
        return new ResponseEntity<>(learningCourseService.createLesson(sectionId, dto), HttpStatus.CREATED);
    }

    @PutMapping("/api/lessons/{lessonId}")
    public ResponseEntity<LessonDTO> updateLesson(@PathVariable Long lessonId, @RequestBody LessonDTO dto) {
        return ResponseEntity.ok(learningCourseService.updateLesson(lessonId, dto));
    }

    @DeleteMapping("/api/lessons/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long lessonId) {
        learningCourseService.deleteLesson(lessonId);
        return ResponseEntity.noContent().build();
    }
}
