package tn.esprit.community.service.impl;

import org.springframework.stereotype.Service;
import tn.esprit.community.dto.request.CourseVoteRequest;
import tn.esprit.community.dto.response.CourseVoteResponse;
import tn.esprit.community.entity.CourseVote;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.repository.CourseVoteRepository;
import tn.esprit.community.service.CourseVoteService;

@Service
public class CourseVoteServiceImpl implements CourseVoteService {
    private final CourseVoteRepository voteRepository;
    private final CourseRepository courseRepository;

    public CourseVoteServiceImpl(CourseVoteRepository voteRepository, CourseRepository courseRepository) {
        this.voteRepository = voteRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public CourseVoteResponse vote(Long courseId, CourseVoteRequest voteRequest) {
        CourseVote existingVote = voteRepository
                .findByCourseIdAndUserId(courseId, voteRequest.getUserId())
                .orElse(null);

        if (existingVote != null && existingVote.getType() == voteRequest.getType()) {
            voteRepository.delete(existingVote);
            return CourseVoteResponse.builder()
                    .id(null)
                    .courseId(courseId)
                    .userId(voteRequest.getUserId())
                    .type(null)
                    .build();
        }

        if (existingVote != null) {
            existingVote.setType(voteRequest.getType());
            return toResponse(voteRepository.save(existingVote));
        }

        CourseVote voteEntity = CourseVote.builder()
                .course(courseRepository.getReferenceById(courseId))
                .userId(voteRequest.getUserId())
                .type(voteRequest.getType())
                .build();
        return toResponse(voteRepository.save(voteEntity));
    }

    private CourseVoteResponse toResponse(CourseVote vote) {
        return CourseVoteResponse.builder()
                .id(vote.getId())
                .courseId(vote.getCourse() != null ? vote.getCourse().getId() : null)
                .userId(vote.getUserId())
                .type(vote.getType())
                .build();
    }
}
