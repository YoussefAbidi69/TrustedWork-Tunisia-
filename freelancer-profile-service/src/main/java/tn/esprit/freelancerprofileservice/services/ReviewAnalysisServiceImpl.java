package tn.esprit.freelancerprofileservice.services;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Implémentation simple d'un moteur de détection d'incohérence
 * entre la note et le commentaire.
 *
 * Exemples :
 * - note 5 avec commentaire fortement négatif => suspect
 * - note 1 avec commentaire fortement positif => suspect
 */
@Service
public class ReviewAnalysisServiceImpl implements ReviewAnalysisService {

    private static final List<String> POSITIVE_WORDS = List.of(
            "excellent", "parfait", "rapide", "professionnel", "professionnelle",
            "super", "top", "incroyable", "génial", "genial", "satisfait",
            "satisfaite", "recommande", "recommandé", "recommandee", "qualité",
            "qualite", "efficace", "très bon", "tres bon", "impeccable"
    );

    private static final List<String> NEGATIVE_WORDS = List.of(
            "mauvais", "nul", "horrible", "terrible", "lent", "arnaque",
            "déçu", "deçu", "decevant", "décevant", "catastrophique",
            "problème", "probleme", "médiocre", "mediocre", "inacceptable",
            "retard", "délais non respectés", "delais non respectes",
            "non professionnel", "non professionnelle"
    );

    @Override
    public ReviewAnalysisResult analyze(Integer rating, String comment) {
        if (rating == null || comment == null || comment.isBlank()) {
            return ReviewAnalysisResult.builder()
                    .flagged(false)
                    .flagReason(null)
                    .build();
        }

        String normalizedComment = normalize(comment);

        boolean containsPositiveWords = containsAny(normalizedComment, POSITIVE_WORDS);
        boolean containsNegativeWords = containsAny(normalizedComment, NEGATIVE_WORDS);

        if (rating >= 4 && containsNegativeWords) {
            return ReviewAnalysisResult.builder()
                    .flagged(true)
                    .flagReason("INCONSISTENT_RATING_COMMENT")
                    .build();
        }

        if (rating <= 2 && containsPositiveWords) {
            return ReviewAnalysisResult.builder()
                    .flagged(true)
                    .flagReason("INCONSISTENT_RATING_COMMENT")
                    .build();
        }

        return ReviewAnalysisResult.builder()
                .flagged(false)
                .flagReason(null)
                .build();
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).trim();
    }
}