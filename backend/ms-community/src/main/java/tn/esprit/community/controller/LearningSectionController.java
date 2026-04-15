package tn.esprit.community.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.lms.SectionDTO;
import tn.esprit.community.service.LearningCourseService;

@RestController
@RequestMapping("/api/sections")
public class LearningSectionController {

    private final LearningCourseService learningCourseService;

    public LearningSectionController(LearningCourseService learningCourseService) {
        this.learningCourseService = learningCourseService;
    }

    @PutMapping("/{sectionId}")
    public ResponseEntity<SectionDTO> updateSection(@PathVariable Long sectionId, @RequestBody SectionDTO dto) {
        return ResponseEntity.ok(learningCourseService.updateSection(sectionId, dto));
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<Void> deleteSection(@PathVariable Long sectionId) {
        learningCourseService.deleteSection(sectionId);
        return ResponseEntity.noContent().build();
    }
}
