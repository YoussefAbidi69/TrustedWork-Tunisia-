package tn.esprit.community.service;

import java.util.List;
import tn.esprit.community.dto.CommunityDTO;

public interface CommunityService {
    CommunityDTO createCommunity(CommunityDTO communityDTO);
    CommunityDTO getCommunity(Long id);
    List<CommunityDTO> listCommunities();
}
