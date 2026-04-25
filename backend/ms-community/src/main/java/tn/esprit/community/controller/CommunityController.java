package tn.esprit.community.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.service.CommunityService;
import tn.esprit.community.dto.request.CommunityRequest;
import tn.esprit.community.dto.response.CommunityResponse;

@RestController
@RequestMapping("/api/communities")
public class CommunityController {
    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping
    public ResponseEntity<CommunityResponse> createCommunity(@RequestBody CommunityRequest communityRequest) {
        CommunityResponse createdCommunity = communityService.createCommunity(communityRequest);
        return new ResponseEntity<>(createdCommunity, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityResponse> getCommunity(@PathVariable Long id) {
        CommunityResponse community = communityService.getCommunity(id);
        return new ResponseEntity<>(community, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<CommunityResponse>> listCommunities() {
        List<CommunityResponse> communities = communityService.listCommunities();
        return new ResponseEntity<>(communities, HttpStatus.OK);
    }
}
