package tn.esprit.msprojectservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Représente un point sur la courbe burndown.
 * Utilisé à la fois pour la courbe idéale et la courbe réelle.
 *
 * Exemple :
 *   { "date": "2026-04-05", "tachesRestantes": 8 }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BurndownPointDTO {

    /** Date du point sur l'axe horizontal */
    private LocalDate date;

    /**
     * Nombre de tâches restantes (non DONE) à cette date.
     * Pour la courbe idéale  : valeur théorique calculée linéairement.
     * Pour la courbe réelle  : valeur historique reconstituée depuis updatedAt des tâches DONE.
     */
    private int tachesRestantes;
}