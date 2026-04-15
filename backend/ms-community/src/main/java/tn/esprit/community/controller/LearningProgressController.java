package tn.esprit.community.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.lms.ProgressDTO;
import tn.esprit.community.dto.lms.ProgressWriteDTO;
import tn.esprit.community.service.LearningProgressService;

@RestController
@RequestMapping("/api/progress")
public class LearningProgressController {

    private final LearningProgressService learningProgressService;

    public LearningProgressController(LearningProgressService learningProgressService) {
        this.learningProgressService = learningProgressService;
    }

    @GetMapping
    public ResponseEntity<ProgressDTO> getProgress(
            @RequestParam Long userId, @RequestParam Long lessonId) {
        return ResponseEntity.ok(learningProgressService.getProgress(userId, lessonId));
    }

    @PostMapping
    public ResponseEntity<ProgressDTO> saveProgress(@RequestBody ProgressWriteDTO dto) {
        return ResponseEntity.ok(learningProgressService.saveProgress(dto));
    }
}
