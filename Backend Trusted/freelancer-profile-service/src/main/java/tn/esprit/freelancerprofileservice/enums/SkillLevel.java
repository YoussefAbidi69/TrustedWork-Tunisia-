package tn.esprit.freelancerprofileservice.enums;

/**
 * Niveau de maîtrise d'une compétence.
 *
 * Utilisé pour :
 * - l'auto-évolution algorithmique
 * - l'affichage frontend (badges, couleurs)
 * - la logique de scoring
 */
public enum SkillLevel {

    JUNIOR("Débutant", 1),
    INTERMEDIATE("Intermédiaire", 2),
    CONFIRMED("Confirmé", 3),
    EXPERT("Expert", 4);

    private final String label;
    private final int rank;

    SkillLevel(String label, int rank) {
        this.label = label;
        this.rank = rank;
    }

    public String getLabel() {
        return label;
    }

    public int getRank() {
        return rank;
    }
}