package tn.esprit.community.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.dto.request.CommunityRequest;
import tn.esprit.community.dto.response.CommunityResponse;
import tn.esprit.community.entity.Community;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.repository.CommunityRepository;
import tn.esprit.community.service.impl.CommunityServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityServiceImplTest {

    @Mock private CommunityRepository communityRepository;

    @InjectMocks
    private CommunityServiceImpl communityService;

    @Test
    @DisplayName("shouldCreateCommunity_whenValidRequest")
    void shouldCreateCommunity_whenValidRequest() {
        CommunityRequest request = CommunityRequest.builder()
                .name("Dev")
                .description("Dev community")
                .createdBy(7L)
                .build();

        Community saved = Community.builder()
                .id(1L)
                .name("Dev")
                .description("Dev community")
                .createdBy(7L)
                .build();
        when(communityRepository.save(any(Community.class))).thenReturn(saved);

        CommunityResponse response = communityService.createCommunity(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Dev");
        assertThat(response.getDescription()).isEqualTo("Dev community");
        assertThat(response.getCreatedBy()).isEqualTo(7L);
    }

    @Test
    @DisplayName("shouldThrowLearningNotFoundException_whenCommunityMissing")
    void shouldThrowLearningNotFoundException_whenCommunityMissing() {
        when(communityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.getCommunity(99L))
                .isInstanceOf(LearningNotFoundException.class)
                .hasMessageContaining("Community not found");
    }

    @Test
    @DisplayName("shouldReturnCommunities_whenListingAll")
    void shouldReturnCommunities_whenListingAll() {
        Community c1 = Community.builder().id(1L).name("A").description("D1").createdBy(1L).build();
        Community c2 = Community.builder().id(2L).name("B").description("D2").createdBy(2L).build();
        when(communityRepository.findAll()).thenReturn(List.of(c1, c2));

        List<CommunityResponse> responses = communityService.listCommunities();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("A");
        assertThat(responses.get(1).getName()).isEqualTo("B");
    }
}
