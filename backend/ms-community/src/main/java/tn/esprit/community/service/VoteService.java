package tn.esprit.community.service;

import tn.esprit.community.dto.VoteDTO;
import tn.esprit.community.entity.Enum.VoteType;

public interface VoteService {
    VoteDTO vote(Long postId, VoteType type, Long userId);
}
