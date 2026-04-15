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
import tn.esprit.community.dto.CommunityDTO;

@RestController
@RequestMapping("/api/communities")
public class CommunityController {
    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping
    public ResponseEntity<CommunityDTO> createCommunity(@RequestBody CommunityDTO communityDTO) {
        CommunityDTO createdCommunity = communityService.createCommunity(communityDTO);
        return new ResponseEntity<>(createdCommunity, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityDTO> getCommunity(@PathVariable Long id) {
        CommunityDTO community = communityService.getCommunity(id);
        return new ResponseEntity<>(community, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<CommunityDTO>> listCommunities() {
        List<CommunityDTO> communities = communityService.listCommunities();
        return new ResponseEntity<>(communities, HttpStatus.OK);
    }
}
