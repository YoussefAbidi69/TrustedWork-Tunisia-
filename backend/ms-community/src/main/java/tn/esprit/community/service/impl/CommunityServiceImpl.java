package tn.esprit.community.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tn.esprit.community.dto.request.CommunityRequest;
import tn.esprit.community.dto.response.CommunityResponse;
import tn.esprit.community.entity.Community;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.repository.CommunityRepository;
import tn.esprit.community.service.CommunityService;

@Service
public class CommunityServiceImpl implements CommunityService {
    private final CommunityRepository communityRepository;

    public CommunityServiceImpl(CommunityRepository communityRepository) {
        this.communityRepository = communityRepository;
    }

    @Override
    public CommunityResponse createCommunity(CommunityRequest communityRequest) {
        Community community = Community.builder()
                .name(communityRequest.getName())
                .description(communityRequest.getDescription())
                .createdBy(communityRequest.getCreatedBy())
                .build();
        return toResponse(communityRepository.save(community));
    }

    @Override
    public CommunityResponse getCommunity(Long id) {
        Community community = communityRepository
                .findById(id)
                .orElseThrow(() -> new LearningNotFoundException("Community not found"));
        return toResponse(community);
    }

    @Override
    public List<CommunityResponse> listCommunities() {
        return communityRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    private CommunityResponse toResponse(Community community) {
        return CommunityResponse.builder()
                .id(community.getId())
                .name(community.getName())
                .description(community.getDescription())
                .createdBy(community.getCreatedBy())
                .build();
    }
}
