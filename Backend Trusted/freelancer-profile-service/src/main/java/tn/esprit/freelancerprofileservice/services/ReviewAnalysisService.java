package tn.esprit.freelancerprofileservice.services;

/**
 * Service d'analyse métier des avis.
 *
 * Objectif :
 * détecter les incohérences simples entre la note et le commentaire,
 * sans utiliser d'IA externe.
 */
public interface ReviewAnalysisService {

    ReviewAnalysisResult analyze(Integer rating, String comment);
}