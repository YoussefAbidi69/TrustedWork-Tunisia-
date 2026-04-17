package tn.esprit.msprojectservice.services;

import tn.esprit.msprojectservice.dto.MLPredictionDTO;

public interface IMLPredictionService {

    /**
     * Prédit le risque de retard d'un projet via le modèle Random Forest.
     * @param projectId ID du projet à analyser
     * @return MLPredictionDTO avec probabilité, décision, sévérité et message
     */
    MLPredictionDTO predictDeliveryRisk(Long projectId);
}