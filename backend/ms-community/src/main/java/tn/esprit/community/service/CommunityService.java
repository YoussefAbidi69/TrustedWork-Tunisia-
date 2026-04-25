package tn.esprit.community.service;

import java.util.List;
import tn.esprit.community.dto.request.CommunityRequest;
import tn.esprit.community.dto.response.CommunityResponse;

public interface CommunityService {
    CommunityResponse createCommunity(CommunityRequest communityRequest);

    CommunityResponse getCommunity(Long id);

    List<CommunityResponse> listCommunities();
}
