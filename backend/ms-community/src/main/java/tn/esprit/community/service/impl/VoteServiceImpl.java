package tn.esprit.community.service.impl;

import org.springframework.stereotype.Service;
import tn.esprit.community.dto.request.VoteRequest;
import tn.esprit.community.dto.response.VoteResponse;
import tn.esprit.community.entity.Vote;
import tn.esprit.community.repository.PostRepository;
import tn.esprit.community.repository.VoteRepository;
import tn.esprit.community.service.VoteService;

@Service
public class VoteServiceImpl implements VoteService {
    private final VoteRepository voteRepository;
    private final PostRepository postRepository;

    public VoteServiceImpl(VoteRepository voteRepository, PostRepository postRepository) {
        this.voteRepository = voteRepository;
        this.postRepository = postRepository;
    }

    @Override
    public VoteResponse vote(Long postId, VoteRequest voteRequest) {
        Vote existingVote = voteRepository
                .findByPostIdAndUserId(postId, voteRequest.getUserId())
                .orElse(null);

        if (existingVote != null && existingVote.getType() == voteRequest.getType()) {
            voteRepository.delete(existingVote);
            return VoteResponse.builder()
                    .id(null)
                    .postId(postId)
                    .userId(voteRequest.getUserId())
                    .type(null)
                    .build();
        }

        if (existingVote != null) {
            existingVote.setType(voteRequest.getType());
            return toResponse(voteRepository.save(existingVote));
        }

        Vote voteEntity = Vote.builder()
                .post(postRepository.getReferenceById(postId))
                .userId(voteRequest.getUserId())
                .type(voteRequest.getType())
                .build();
        return toResponse(voteRepository.save(voteEntity));
    }

    private VoteResponse toResponse(Vote vote) {
        return VoteResponse.builder()
                .id(vote.getId())
                .postId(vote.getPost() != null ? vote.getPost().getId() : null)
                .userId(vote.getUserId())
                .type(vote.getType())
                .build();
    }
}
