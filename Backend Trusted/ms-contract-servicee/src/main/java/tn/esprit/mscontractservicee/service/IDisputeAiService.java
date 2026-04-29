package tn.esprit.mscontractservicee.service;

import tn.esprit.mscontractservicee.dto.dispute.DisputeAiRecommendation;

public interface IDisputeAiService {

    /**
     * Analyse le litige et retourne une recommandation AI stateless.
     * Ne persiste rien en base.
     *
     * @param disputeId  ID du litige à analyser
     * @param adminCin   CIN de l'admin qui demande l'analyse
     * @return           Recommandation AI à la volée
     */
    DisputeAiRecommendation analyze(Long disputeId, Long adminCin);
}
