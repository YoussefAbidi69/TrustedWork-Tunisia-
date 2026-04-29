package tn.esprit.community.service;

import tn.esprit.community.dto.request.CourseVoteRequest;
import tn.esprit.community.dto.response.CourseVoteResponse;

public interface CourseVoteService {
    CourseVoteResponse vote(Long courseId, CourseVoteRequest voteRequest);
}
