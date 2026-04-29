package tn.esprit.community.service;

import tn.esprit.community.dto.request.VoteRequest;
import tn.esprit.community.dto.response.VoteResponse;

public interface VoteService {
    VoteResponse vote(Long postId, VoteRequest voteRequest);
}
