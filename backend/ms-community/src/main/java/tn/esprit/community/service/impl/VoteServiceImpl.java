package tn.esprit.community.service.impl;

import org.springframework.stereotype.Service;
import tn.esprit.community.dto.VoteDTO;
import tn.esprit.community.entity.Vote;
import tn.esprit.community.mapper.VoteMapper;
import tn.esprit.community.repository.PostRepository;
import tn.esprit.community.repository.VoteRepository;
import tn.esprit.community.service.VoteService;
import tn.esprit.community.entity.Enum.VoteType;

@Service
public class VoteServiceImpl implements VoteService {
    private final VoteRepository voteRepository;
    private final PostRepository postRepository;
    private final VoteMapper voteMapper;

    public VoteServiceImpl(VoteRepository voteRepository, PostRepository postRepository, VoteMapper voteMapper) {
        this.voteRepository = voteRepository;
        this.postRepository = postRepository;
        this.voteMapper = voteMapper;
    }

    @Override
    public VoteDTO vote(Long postId, VoteType type, Long userId) {
        Vote existingVote = voteRepository.findByPost_IdAndUserId(postId, userId).orElse(null);
        if (existingVote != null && existingVote.getType() == type) {
            voteRepository.delete(existingVote);
            return VoteDTO.builder()
                    .id(null)
                    .postId(postId)
                    .userId(userId)
                    .type(null)
                    .build();
        }
        if (existingVote != null) {
            existingVote.setType(type);
            return voteMapper.toDto(voteRepository.save(existingVote));
        }
        Vote voteEntity = Vote.builder()
                .post(postRepository.getReferenceById(postId))
                .userId(userId)
                .type(type)
                .build();
        return voteMapper.toDto(voteRepository.save(voteEntity));
    }
}
