package tn.esprit.community.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.lms.CourseDTO;
import tn.esprit.community.dto.lms.SectionDTO;
import tn.esprit.community.service.LearningCourseService;

@RestController
@RequestMapping("/api/courses")
public class LearningCourseController {

    private final LearningCourseService learningCourseService;

    public LearningCourseController(LearningCourseService learningCourseService) {
        this.learningCourseService = learningCourseService;
    }

    @GetMapping
    public ResponseEntity<List<CourseDTO>> listCourses(
            @RequestParam Long communityId, @RequestParam(defaultValue = "true") boolean publishedOnly) {
        return ResponseEntity.ok(learningCourseService.listCoursesByCommunity(communityId, publishedOnly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDTO> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(learningCourseService.getCourse(id));
    }

    @PostMapping
    public ResponseEntity<CourseDTO> createCourse(@RequestBody CourseDTO dto) {
        return new ResponseEntity<>(learningCourseService.createCourse(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseDTO> updateCourse(@PathVariable Long id, @RequestBody CourseDTO dto) {
        return ResponseEntity.ok(learningCourseService.updateCourse(id, dto));
    }

    @GetMapping("/{courseId}/sections")
    public ResponseEntity<List<SectionDTO>> listSections(@PathVariable Long courseId) {
        return ResponseEntity.ok(learningCourseService.listSections(courseId));
    }

    @PostMapping("/{courseId}/sections")
    public ResponseEntity<SectionDTO> createSection(@PathVariable Long courseId, @RequestBody SectionDTO dto) {
        return new ResponseEntity<>(learningCourseService.createSection(courseId, dto), HttpStatus.CREATED);
    }
}
