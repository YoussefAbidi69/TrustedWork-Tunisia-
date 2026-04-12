package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.entities.PortfolioItem;

import java.util.List;

public interface IPortfolioService {
    PortfolioItem addPortfolioItem(Long userId, PortfolioItem item);
    List<PortfolioItem> getMyPortfolio(Long userId);
    PortfolioItem updatePortfolioItem(Long itemId, Long userId, PortfolioItem updates);
    void deletePortfolioItem(Long itemId, Long userId);
}