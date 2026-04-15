package tn.esprit.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.VoteDTO;
import tn.esprit.community.service.VoteService;
import tn.esprit.community.entity.Enum.VoteType;

@RestController
@RequestMapping("/api/votes")
public class VoteController {
    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping
    public ResponseEntity<VoteDTO> vote(
            @RequestParam Long postId,
            @RequestParam VoteType type,
            @RequestParam Long userId) {
        VoteDTO vote = voteService.vote(postId, type, userId);
        return new ResponseEntity<>(vote, HttpStatus.OK);
    }
}
