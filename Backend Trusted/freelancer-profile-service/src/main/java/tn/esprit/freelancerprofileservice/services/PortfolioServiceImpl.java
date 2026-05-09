package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.PortfolioItem;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.PortfolioItemRepository;

import java.util.List;

/**
 * Implémentation du service de gestion du portfolio
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PortfolioServiceImpl implements IPortfolioService {

    private static final int MAX_PORTFOLIO_ITEMS = 20;
    private static final int MAX_PINNED_ITEMS = 3;

    private final PortfolioItemRepository portfolioItemRepository;
    private final FreelancerProfileRepository profileRepository;
    private final ICompletenessService completenessService;
    private final ISkillAuthenticityService skillAuthenticityService;

    @Override
    public PortfolioItem addPortfolioItem(Long userId, PortfolioItem item) {
        FreelancerProfile profile = getProfileByUserId(userId);

        long totalItems = portfolioItemRepository.countByProfileId(profile.getId());
        if (totalItems >= MAX_PORTFOLIO_ITEMS) {
            throw new InvalidDataException("Vous avez atteint la limite de 20 projets portfolio");
        }

        validateDuplicateTitleForCreate(profile.getId(), item.getTitle());

        if (Boolean.TRUE.equals(item.getPinned())) {
            validatePinnedLimit(profile.getId());
        } else {
            item.setPinned(false);
        }

        item.setProfile(profile);

        PortfolioItem savedItem = portfolioItemRepository.save(item);
        refreshProfileScores(userId, profile.getId());

        return savedItem;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioItem> getMyPortfolio(Long userId) {
        FreelancerProfile profile = getProfileByUserId(userId);
        return portfolioItemRepository.findByProfileIdOrderByPinnedDescCompletionDateDescIdDesc(profile.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioItem> getPinnedPortfolio(Long userId) {
        FreelancerProfile profile = getProfileByUserId(userId);
        return portfolioItemRepository.findByProfileIdAndPinnedTrueOrderByCompletionDateDescIdDesc(profile.getId());
    }

    @Override
    public PortfolioItem updatePortfolioItem(Long itemId, Long userId, PortfolioItem updates) {
        PortfolioItem item = getOwnedPortfolioItem(itemId, userId);
        Long profileId = item.getProfile().getId();

        validateDuplicateTitleForUpdate(profileId, updates.getTitle(), itemId);

        boolean wantsPinned = Boolean.TRUE.equals(updates.getPinned());
        boolean currentlyPinned = Boolean.TRUE.equals(item.getPinned());

        if (!currentlyPinned && wantsPinned) {
            validatePinnedLimit(profileId);
        }

        item.setTitle(updates.getTitle());
        item.setDescription(updates.getDescription());
        item.setProjectUrl(updates.getProjectUrl());
        item.setImageUrl(updates.getImageUrl());
        item.setTechnologies(updates.getTechnologies());
        item.setCompletionDate(updates.getCompletionDate());
        item.setPinned(wantsPinned);

        PortfolioItem savedItem = portfolioItemRepository.save(item);
        refreshProfileScores(userId, profileId);

        return savedItem;
    }

    @Override
    public PortfolioItem pinPortfolioItem(Long itemId, Long userId) {
        PortfolioItem item = getOwnedPortfolioItem(itemId, userId);

        if (Boolean.TRUE.equals(item.getPinned())) {
            return item;
        }

        validatePinnedLimit(item.getProfile().getId());

        item.setPinned(true);
        PortfolioItem savedItem = portfolioItemRepository.save(item);
        refreshProfileScores(userId, item.getProfile().getId());

        return savedItem;
    }

    @Override
    public PortfolioItem unpinPortfolioItem(Long itemId, Long userId) {
        PortfolioItem item = getOwnedPortfolioItem(itemId, userId);

        if (!Boolean.TRUE.equals(item.getPinned())) {
            return item;
        }

        item.setPinned(false);
        PortfolioItem savedItem = portfolioItemRepository.save(item);
        refreshProfileScores(userId, item.getProfile().getId());

        return savedItem;
    }

    @Override
    public void deletePortfolioItem(Long itemId, Long userId) {
        PortfolioItem item = getOwnedPortfolioItem(itemId, userId);
        Long profileId = item.getProfile().getId();

        portfolioItemRepository.delete(item);
        refreshProfileScores(userId, profileId);
    }

    private FreelancerProfile getProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil introuvable"));
    }

    private PortfolioItem getOwnedPortfolioItem(Long itemId, Long userId) {
        FreelancerProfile profile = getProfileByUserId(userId);

        return portfolioItemRepository.findByIdAndProfileId(itemId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable ou non autorisé"));
    }

    private void validateDuplicateTitleForCreate(Long profileId, String title) {
        if (title == null || title.isBlank()) {
            return;
        }

        boolean exists = portfolioItemRepository.existsByProfileIdAndTitleIgnoreCase(profileId, title.trim());
        if (exists) {
            throw new DuplicateResourceException("Un projet avec ce titre existe déjà dans votre portfolio");
        }
    }

    private void validateDuplicateTitleForUpdate(Long profileId, String title, Long itemId) {
        if (title == null || title.isBlank()) {
            return;
        }

        boolean exists = portfolioItemRepository.existsByProfileIdAndTitleIgnoreCaseAndIdNot(profileId, title.trim(), itemId);
        if (exists) {
            throw new DuplicateResourceException("Un projet avec ce titre existe déjà dans votre portfolio");
        }
    }

    private void validatePinnedLimit(Long profileId) {
        long pinnedCount = portfolioItemRepository.countByProfileIdAndPinnedTrue(profileId);
        if (pinnedCount >= MAX_PINNED_ITEMS) {
            throw new InvalidDataException("Vous ne pouvez pas épingler plus de 3 projets");
        }
    }

    private void refreshProfileScores(Long userId, Long profileId) {
        completenessService.calculateCompleteness(userId);
        skillAuthenticityService.recalculateAllScores(profileId);
    }
}