package tn.esprit.community.community;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tn.esprit.community.dto.CommunityDTO;

@Service
public class CommunityServiceImpl implements CommunityService {
    private final CommunityRepository communityRepository;

    public CommunityServiceImpl(CommunityRepository communityRepository) {
        this.communityRepository = communityRepository;
    }

    @Override
    public CommunityDTO createCommunity(CommunityDTO communityDTO) {
        Community community = Community.builder()
                .name(communityDTO.getName())
                .description(communityDTO.getDescription())
                .createdBy(communityDTO.getCreatedBy())
                .build();
        community = communityRepository.save(community);
        communityDTO.setId(community.getId());
        return communityDTO;
    }

    @Override
    public CommunityDTO getCommunity(Long id) {
        Community community = communityRepository.findById(id).orElse(null);
        return community == null ? null : CommunityDTO.builder().id(community.getId()).name(community.getName()).description(community.getDescription()).createdBy(community.getCreatedBy()).build();
    }

    @Override
    public List<CommunityDTO> listCommunities() {
        return communityRepository.findAll().stream().map(c -> CommunityDTO.builder().id(c.getId()).name(c.getName()).description(c.getDescription()).createdBy(c.getCreatedBy()).build()).collect(Collectors.toList());
    }
}
