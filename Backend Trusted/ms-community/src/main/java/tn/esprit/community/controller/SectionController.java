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
import tn.esprit.community.dto.request.SectionRequest;
import tn.esprit.community.dto.response.SectionResponse;
import tn.esprit.community.service.SectionService;

@RestController
@RequestMapping("/api/sections")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @PostMapping("/course/{courseId}")
    public ResponseEntity<SectionResponse> createSection(
            @PathVariable Long courseId, @RequestBody SectionRequest sectionRequest) {
        return new ResponseEntity<>(sectionService.createSection(courseId, sectionRequest), HttpStatus.CREATED);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<SectionResponse>> listSections(@PathVariable Long courseId) {
        return new ResponseEntity<>(sectionService.listSections(courseId), HttpStatus.OK);
    }

    @PutMapping("/{sectionId}")
    public ResponseEntity<SectionResponse> updateSection(
            @PathVariable Long sectionId, @RequestBody SectionRequest sectionRequest) {
        return new ResponseEntity<>(sectionService.updateSection(sectionId, sectionRequest), HttpStatus.OK);
    }

    @GetMapping("/{sectionId}")
    public ResponseEntity<SectionResponse> getSection(@PathVariable Long sectionId) {
        return new ResponseEntity<>(sectionService.getSection(sectionId), HttpStatus.OK);
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<Void> deleteSection(@PathVariable Long sectionId) {
        sectionService.deleteSection(sectionId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
