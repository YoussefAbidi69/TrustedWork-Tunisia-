package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.PortfolioItem;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.PortfolioItemRepository;

import java.util.List;

/**
 * Implémentation du service de gestion du portfolio
 */
@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements IPortfolioService {

    private final PortfolioItemRepository portfolioItemRepository;
    private final FreelancerProfileRepository profileRepository;

    @Override
    public PortfolioItem addPortfolioItem(Long userId, PortfolioItem item) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));
        item.setProfile(profile);
        return portfolioItemRepository.save(item);
    }

    @Override
    public List<PortfolioItem> getMyPortfolio(Long userId) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));
        return portfolioItemRepository.findByProfileId(profile.getId());
    }

    @Override
    public PortfolioItem updatePortfolioItem(Long itemId, Long userId, PortfolioItem updates) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));
        if (!item.getProfile().getUserId().equals(userId)) {
            throw new RuntimeException("Action non autorisée");
        }
        item.setTitle(updates.getTitle());
        item.setDescription(updates.getDescription());
        item.setProjectUrl(updates.getProjectUrl());
        item.setImageUrl(updates.getImageUrl());
        item.setTechnologies(updates.getTechnologies());
        item.setCompletionDate(updates.getCompletionDate());
        return portfolioItemRepository.save(item);
    }

    @Override
    public void deletePortfolioItem(Long itemId, Long userId) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));
        if (!item.getProfile().getUserId().equals(userId)) {
            throw new RuntimeException("Action non autorisée");
        }
        portfolioItemRepository.delete(item);
    }
}