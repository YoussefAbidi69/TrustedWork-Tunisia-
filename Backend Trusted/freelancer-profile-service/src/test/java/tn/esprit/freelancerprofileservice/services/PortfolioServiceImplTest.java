package tn.esprit.freelancerprofileservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.PortfolioItem;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.PortfolioItemRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    @Mock private PortfolioItemRepository portfolioItemRepository;
    @Mock private FreelancerProfileRepository profileRepository;
    @Mock private ICompletenessService completenessService;
    @Mock private ISkillAuthenticityService skillAuthenticityService;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    private FreelancerProfile profile;
    private PortfolioItem item;

    @BeforeEach
    void setUp() {
        profile = FreelancerProfile.builder()
                .id(1L)
                .userId(100L)
                .headline("Full Stack Developer")
                .build();

        item = PortfolioItem.builder()
                .id(10L)
                .title("E-Commerce Platform")
                .description("Built with Spring Boot and Angular")
                .projectUrl("https://github.com/user/ecommerce")
                .technologies("Java, Spring Boot, Angular")
                .completionDate(LocalDate.of(2024, 3, 1))
                .pinned(false)
                .profile(profile)
                .build();
    }

    // ─── addPortfolioItem ────────────────────────────────────────────────────

    @Test
    void addPortfolioItem_shouldSaveAndReturn() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(portfolioItemRepository.countByProfileId(1L)).thenReturn(0L);
        when(portfolioItemRepository.existsByProfileIdAndTitleIgnoreCase(any(), any())).thenReturn(false);
        when(portfolioItemRepository.save(any())).thenReturn(item);

        PortfolioItem result = portfolioService.addPortfolioItem(100L, item);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("E-Commerce Platform");
        verify(portfolioItemRepository).save(any());
        verify(completenessService).calculateCompleteness(100L);
    }

    @Test
    void addPortfolioItem_shouldThrow_whenProfileNotFound() {
        when(profileRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.addPortfolioItem(99L, item))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addPortfolioItem_shouldThrow_whenLimitReached() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(portfolioItemRepository.countByProfileId(1L)).thenReturn(20L);

        assertThatThrownBy(() -> portfolioService.addPortfolioItem(100L, item))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("limite de 20");
    }

    @Test
    void addPortfolioItem_shouldThrow_whenTitleDuplicate() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(portfolioItemRepository.countByProfileId(1L)).thenReturn(3L);
        when(portfolioItemRepository.existsByProfileIdAndTitleIgnoreCase(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> portfolioService.addPortfolioItem(100L, item))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("titre existe déjà");
    }

    @Test
    void addPortfolioItem_shouldThrow_whenPinnedLimitReached() {
        item.setPinned(true);

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(portfolioItemRepository.countByProfileId(1L)).thenReturn(5L);
        when(portfolioItemRepository.existsByProfileIdAndTitleIgnoreCase(any(), any())).thenReturn(false);
        when(portfolioItemRepository.countByProfileIdAndPinnedTrue(1L)).thenReturn(3L);

        assertThatThrownBy(() -> portfolioService.addPortfolioItem(100L, item))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("épingler plus de 3");
    }

    // ─── getMyPortfolio ──────────────────────────────────────────────────────

    @Test
    void getMyPortfolio_shouldReturnList() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(portfolioItemRepository.findByProfileIdOrderByPinnedDescCompletionDateDescIdDesc(1L)).thenReturn(List.of(item));

        List<PortfolioItem> result = portfolioService.getMyPortfolio(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("E-Commerce Platform");
    }

    // ─── getPinnedPortfolio ──────────────────────────────────────────────────

    @Test
    void getPinnedPortfolio_shouldReturnOnlyPinnedItems() {
        PortfolioItem pinned = PortfolioItem.builder()
                .id(11L).title("Pinned Project").pinned(true).profile(profile).build();

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(portfolioItemRepository.findByProfileIdAndPinnedTrueOrderByCompletionDateDescIdDesc(1L)).thenReturn(List.of(pinned));

        List<PortfolioItem> result = portfolioService.getPinnedPortfolio(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPinned()).isTrue();
    }

    // ─── pinPortfolioItem ────────────────────────────────────────────────────

    @Test
    void pinPortfolioItem_shouldPin_whenNotAlreadyPinned() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(portfolioItemRepository.findByIdAndProfileId(10L, 1L)).thenReturn(Optional.of(item));
        when(portfolioItemRepository.countByProfileIdAndPinnedTrue(1L)).thenReturn(1L);
        when(portfolioItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PortfolioItem result = portfolioService.pinPortfolioItem(10L, 100L);

        assertThat(result.getPinned()).isTrue();
        verify(portfolioItemRepository).save(any());
    }

    @Test
    void pinPortfolioItem_shouldReturnAsIs_whenAlreadyPinned() {
        item.setPinned(true);

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(portfolioItemRepository.findByIdAndProfileId(10L, 1L)).thenReturn(Optional.of(item));

        PortfolioItem result = portfolioService.pinPortfolioItem(10L, 100L);

        assertThat(result.getPinned()).isTrue();
        verify(portfolioItemRepository, never()).save(any());
    }

    // ─── unpinPortfolioItem ──────────────────────────────────────────────────

    @Test
    void unpinPortfolioItem_shouldUnpin() {
        item.setPinned(true);

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(portfolioItemRepository.findByIdAndProfileId(10L, 1L)).thenReturn(Optional.of(item));
        when(portfolioItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PortfolioItem result = portfolioService.unpinPortfolioItem(10L, 100L);

        assertThat(result.getPinned()).isFalse();
    }

    @Test
    void unpinPortfolioItem_shouldReturnAsIs_whenAlreadyUnpinned() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(portfolioItemRepository.findByIdAndProfileId(10L, 1L)).thenReturn(Optional.of(item));

        PortfolioItem result = portfolioService.unpinPortfolioItem(10L, 100L);

        assertThat(result.getPinned()).isFalse();
        verify(portfolioItemRepository, never()).save(any());
    }

    // ─── deletePortfolioItem ─────────────────────────────────────────────────

    @Test
    void deletePortfolioItem_shouldDeleteSuccessfully() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(portfolioItemRepository.findByIdAndProfileId(10L, 1L)).thenReturn(Optional.of(item));

        portfolioService.deletePortfolioItem(10L, 100L);

        verify(portfolioItemRepository).delete(item);
        verify(completenessService).calculateCompleteness(100L);
        verify(skillAuthenticityService).recalculateAllScores(1L);
    }

    @Test
    void deletePortfolioItem_shouldThrow_whenNotFound() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(portfolioItemRepository.findByIdAndProfileId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.deletePortfolioItem(99L, 100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("introuvable");
    }
}
