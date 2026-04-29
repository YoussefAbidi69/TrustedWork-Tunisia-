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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.request.CourseRequest;
import tn.esprit.community.dto.response.CourseDownloadResponse;
import tn.esprit.community.dto.response.CourseResponse;
import tn.esprit.community.service.CourseService;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@RequestBody CourseRequest courseRequest) {
        return new ResponseEntity<>(courseService.createCourse(courseRequest), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable Long id) {
        return new ResponseEntity<>(courseService.getCourse(id), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> listCourses(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) Boolean publishedOnly) {
        return new ResponseEntity<>(courseService.listCourses(communityId, publishedOnly), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable Long id, @RequestBody CourseRequest courseRequest) {
        return new ResponseEntity<>(courseService.updateCourse(id, courseRequest), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<CourseDownloadResponse> downloadCourse(@PathVariable("id") Long courseId) {
        return new ResponseEntity<>(courseService.downloadCourse(courseId), HttpStatus.OK);
    }
}
