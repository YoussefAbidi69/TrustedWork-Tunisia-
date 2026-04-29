package tn.esprit.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.request.CourseVoteRequest;
import tn.esprit.community.dto.response.CourseVoteResponse;
import tn.esprit.community.service.CourseVoteService;

@RestController
@RequestMapping("/api/course-votes")
public class CourseVoteController {
    private final CourseVoteService voteService;

    public CourseVoteController(CourseVoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/course/{courseId}")
    public ResponseEntity<CourseVoteResponse> vote(@PathVariable Long courseId, @RequestBody CourseVoteRequest voteRequest) {
        CourseVoteResponse vote = voteService.vote(courseId, voteRequest);
        return new ResponseEntity<>(vote, HttpStatus.OK);
    }
}
