package tn.esprit.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.request.VoteRequest;
import tn.esprit.community.dto.response.VoteResponse;
import tn.esprit.community.service.VoteService;

@RestController
@RequestMapping("/api/votes")
public class VoteController {
    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/post/{postId}")
    public ResponseEntity<VoteResponse> vote(@PathVariable Long postId, @RequestBody VoteRequest voteRequest) {
        VoteResponse vote = voteService.vote(postId, voteRequest);
        return new ResponseEntity<>(vote, HttpStatus.OK);
    }
}
