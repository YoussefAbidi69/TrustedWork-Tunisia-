package tn.esprit.community.vote;

import tn.esprit.community.dto.VoteDTO;

public interface VoteService {
    VoteDTO vote(Long postId, VoteType type, Long userId);
}
