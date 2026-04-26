package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.dto.response.CompletenessResponse;

public interface ICompletenessService {

    // Calculer et retourner le score de complétude avec suggestions
    CompletenessResponse calculateCompleteness(Long userId);
}