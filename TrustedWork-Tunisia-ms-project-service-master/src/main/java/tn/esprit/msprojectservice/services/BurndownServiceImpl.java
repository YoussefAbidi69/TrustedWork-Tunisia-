package tn.esprit.msprojectservice.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.esprit.msprojectservice.dto.BurndownChartDTO;
import tn.esprit.msprojectservice.dto.BurndownPointDTO;
import tn.esprit.msprojectservice.entities.Project;
import tn.esprit.msprojectservice.entities.Task;
import tn.esprit.msprojectservice.entities.TaskStatus;
import tn.esprit.msprojectservice.exceptions.EntityNotFoundException;
import tn.esprit.msprojectservice.repositories.IProjectRepository;
import tn.esprit.msprojectservice.repositories.ITaskRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implémentation du service Burndown Chart.
 *
 * Logique :
 *   1. Courbe idéale  : décroissance linéaire de totalTaches → 0 sur toute la durée du projet
 *   2. Courbe réelle  : reconstituée depuis les updatedAt des tâches DONE (pour les dates passées)
 *                       + projetée par régression linéaire (pour les dates futures)
 *   3. Métriques      : retard, vélocité, date projetée, statut, message d'analyse
 */
@Service
@RequiredArgsConstructor
public class BurndownServiceImpl implements IBurndownService {

    private static final Logger logger = LoggerFactory.getLogger(BurndownServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final IProjectRepository projectRepository;
    private final ITaskRepository    taskRepository;

    // ============================================================
    // POINT D'ENTRÉE PRINCIPAL
    // ============================================================

    @Override
    public BurndownChartDTO getBurndownChart(Long projectId) {
        logger.info("Calcul du burndown chart pour le projet ID: {}", projectId);

        // 1. Récupérer le projet
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Projet introuvable avec l'id : " + projectId));

        // 2. Vérifier que les dates sont définies
        if (project.getStartDate() == null || project.getEndDate() == null) {
            throw new IllegalStateException(
                    "Le projet \"" + project.getTitle() + "\" n'a pas de startDate ou endDate définis. " +
                            "Le burndown chart nécessite ces deux dates."
            );
        }

        // 3. Récupérer toutes les tâches du projet
        List<Task> toutesLesTaches = taskRepository.findByProjectId(projectId);
        int totalTaches = toutesLesTaches.size();

        if (totalTaches == 0) {
            logger.warn("Aucune tâche trouvée pour le projet {} — burndown vide retourné", projectId);
            return buildEmptyBurndown(project);
        }

        // 4. Isoler les tâches DONE avec leur date de complétion (updatedAt)
        List<Task> tachesDone = toutesLesTaches.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .toList();

        // 5. Construire la map : date → nombre de tâches completées CE JOUR-LÀ
        Map<LocalDate, Long> completionsParJour = tachesDone.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getUpdatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        // 6. Générer les deux courbes
        List<BurndownPointDTO> courbeIdeale = genererCourbeIdeale(
                project.getStartDate(), project.getEndDate(), totalTaches);

        List<BurndownPointDTO> courbeReelle = genererCourbeReelle(
                project.getStartDate(), project.getEndDate(), totalTaches,
                completionsParJour, tachesDone.size());

        // 7. Calculer les métriques
        int tachesRestantes = totalTaches - tachesDone.size();
        double velocite     = calculerVelocite(project.getStartDate(), tachesDone.size());
        LocalDate dateProjete = calculerDateProjectee(tachesRestantes, velocite);
        int retard          = calculerRetardJours(project, totalTaches, tachesDone.size());
        String statut       = determinerStatut(retard);
        String analyse      = genererAnalyse(project, totalTaches, tachesDone.size(),
                tachesRestantes, velocite, dateProjete, retard);

        logger.info("Burndown calculé — Projet: {} | Tâches restantes: {} | Vélocité: {}/j | Retard: {} j",
                project.getTitle(), tachesRestantes, velocite, retard);

        return BurndownChartDTO.builder()
                .projectId(projectId)
                .projectTitle(project.getTitle())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .totalTaches(totalTaches)
                .courbeIdeale(courbeIdeale)
                .courbeReelle(courbeReelle)
                .tachesRestantesAujourdhui(tachesRestantes)
                .tachesCompletees(tachesDone.size())
                .retardEstimeJours(retard)
                .velociteJournaliere(Math.round(velocite * 100.0) / 100.0)
                .dateLivraisonProjetee(dateProjete)
                .statutBurndown(statut)
                .analyse(analyse)
                .build();
    }

    // ============================================================
    // COURBE IDÉALE
    // Décroissance linéaire parfaite : totalTaches → 0 sur toute la durée
    // ============================================================

    private List<BurndownPointDTO> genererCourbeIdeale(
            LocalDate startDate, LocalDate endDate, int totalTaches) {

        List<BurndownPointDTO> courbe = new ArrayList<>();
        long dureeTotale = ChronoUnit.DAYS.between(startDate, endDate);

        if (dureeTotale <= 0) {
            courbe.add(BurndownPointDTO.builder()
                    .date(startDate)
                    .tachesRestantes(totalTaches)
                    .build());
            return courbe;
        }

        // Générer un point par jour de startDate à endDate inclus
        for (long jour = 0; jour <= dureeTotale; jour++) {
            LocalDate date = startDate.plusDays(jour);

            // Formule de décroissance linéaire
            // tachesIdeales(jour) = totalTaches - (totalTaches × jour / dureeTotale)
            int tachesIdeales = (int) Math.round(
                    totalTaches - ((double) totalTaches * jour / dureeTotale)
            );

            courbe.add(BurndownPointDTO.builder()
                    .date(date)
                    .tachesRestantes(Math.max(0, tachesIdeales))
                    .build());
        }

        return courbe;
    }

    // ============================================================
    // COURBE RÉELLE
    // Pour les dates passées : reconstituée depuis les completions réelles
    // Pour les dates futures : projetée par tendance linéaire
    // ============================================================

    private List<BurndownPointDTO> genererCourbeReelle(
            LocalDate startDate, LocalDate endDate, int totalTaches,
            Map<LocalDate, Long> completionsParJour, int totalDone) {

        List<BurndownPointDTO> courbe = new ArrayList<>();
        LocalDate today = LocalDate.now();

        long dureeTotale = ChronoUnit.DAYS.between(startDate, endDate);
        if (dureeTotale <= 0) return courbe;

        // ---- Partie passée : données réelles ----
        int tachesRestantes = totalTaches; // on part du total et on décrémente

        // Borner la partie passée : de startDate à min(today, endDate)
        LocalDate finPartiePassee = today.isBefore(endDate) ? today : endDate;

        for (long jour = 0; jour <= ChronoUnit.DAYS.between(startDate, finPartiePassee); jour++) {
            LocalDate date = startDate.plusDays(jour);

            // Soustraire les tâches completées CE jour-là
            long completeesCeJour = completionsParJour.getOrDefault(date, 0L);
            tachesRestantes -= (int) completeesCeJour;
            tachesRestantes  = Math.max(0, tachesRestantes); // jamais négatif

            courbe.add(BurndownPointDTO.builder()
                    .date(date)
                    .tachesRestantes(tachesRestantes)
                    .build());
        }

        // ---- Partie future : projection linéaire ----
        // Si on est avant la fin du projet, on projette la tendance
        if (today.isBefore(endDate)) {
            long joursEcoules = ChronoUnit.DAYS.between(startDate, today);
            double velocite   = joursEcoules > 0
                    ? (double) totalDone / joursEcoules
                    : 0.0;

            int tachesActuelles = tachesRestantes;
            long joursRestants  = ChronoUnit.DAYS.between(today, endDate);

            for (long j = 1; j <= joursRestants; j++) {
                LocalDate dateFuture = today.plusDays(j);
                int projection = (int) Math.max(0,
                        Math.round(tachesActuelles - (velocite * j)));

                courbe.add(BurndownPointDTO.builder()
                        .date(dateFuture)
                        .tachesRestantes(projection)
                        .build());
            }
        }

        return courbe;
    }

    // ============================================================
    // MÉTRIQUES
    // ============================================================

    /**
     * Vélocité journalière = tâches DONE / jours écoulés depuis startDate.
     * Minimum 0.01 pour éviter la division par zéro dans les projections.
     */
    private double calculerVelocite(LocalDate startDate, int totalDone) {
        long joursEcoules = ChronoUnit.DAYS.between(startDate, LocalDate.now());
        if (joursEcoules <= 0 || totalDone == 0) return 0.0;
        return (double) totalDone / joursEcoules;
    }

    /**
     * Date de livraison projetée basée sur la vélocité actuelle.
     * Si vélocité = 0, retourne null (impossible de projeter).
     */
    private LocalDate calculerDateProjectee(int tachesRestantes, double velocite) {
        if (velocite <= 0 || tachesRestantes <= 0) return LocalDate.now();
        long joursNecessaires = (long) Math.ceil(tachesRestantes / velocite);
        return LocalDate.now().plusDays(joursNecessaires);
    }

    /**
     * Retard en jours = date projetée - endDate du projet.
     * Positif = en retard. Négatif = en avance.
     */
    private int calculerRetardJours(Project project, int totalTaches, int totalDone) {
        int tachesRestantes = totalTaches - totalDone;
        if (tachesRestantes <= 0) return 0; // projet terminé, pas de retard

        long joursEcoules = ChronoUnit.DAYS.between(project.getStartDate(), LocalDate.now());
        double velocite   = joursEcoules > 0 ? (double) totalDone / joursEcoules : 0.0;

        if (velocite <= 0) {
            // Aucun avancement — retard = jours jusqu'à la fin du projet
            return (int) ChronoUnit.DAYS.between(LocalDate.now(), project.getEndDate()) * -1;
        }

        LocalDate dateProjete = calculerDateProjectee(tachesRestantes, velocite);
        return (int) ChronoUnit.DAYS.between(project.getEndDate(), dateProjete);
    }

    /**
     * Détermine le statut du burndown selon le retard.
     */
    private String determinerStatut(int retardJours) {
        if (retardJours <= 0) {
            return retardJours <= -3 ? "EN_AVANCE" : "DANS_LES_DELAIS";
        } else if (retardJours <= 5) {
            return "EN_RETARD";
        } else {
            return "CRITIQUE";
        }
    }

    /**
     * Génère un message d'analyse contextuel en français.
     */
    private String genererAnalyse(Project project, int totalTaches, int totalDone,
                                  int tachesRestantes, double velocite,
                                  LocalDate dateProjete, int retardJours) {

        String endDateFormatee    = project.getEndDate().format(DATE_FORMATTER);
        String projeteFormatee    = dateProjete != null ? dateProjete.format(DATE_FORMATTER) : "inconnue";
        int    pourcentage        = totalTaches > 0 ? (totalDone * 100 / totalTaches) : 0;
        String velociteFormatee   = String.format("%.1f", velocite);

        // Projet terminé
        if (tachesRestantes <= 0) {
            return String.format(
                    "✅ Projet \"%s\" terminé ! Toutes les %d tâches ont été complétées. " +
                            "Félicitations pour avoir respecté les engagements.",
                    project.getTitle(), totalTaches
            );
        }

        // En avance
        if (retardJours <= -3) {
            return String.format(
                    "🟢 Excellent rythme sur le projet \"%s\" ! " +
                            "À votre vélocité actuelle de %s tâches/jour, " +
                            "la livraison est projetée au %s, soit %d jours avant la date prévue du %s. " +
                            "Avancement actuel : %d%% (%d/%d tâches complétées).",
                    project.getTitle(), velociteFormatee, projeteFormatee,
                    Math.abs(retardJours), endDateFormatee,
                    pourcentage, totalDone, totalTaches
            );
        }

        // Dans les délais
        if (retardJours <= 0) {
            return String.format(
                    "🟡 Le projet \"%s\" avance conformément au planning. " +
                            "Vélocité : %s tâches/jour. Livraison projetée au %s (deadline : %s). " +
                            "Avancement : %d%% — %d tâches restantes sur %d.",
                    project.getTitle(), velociteFormatee, projeteFormatee,
                    endDateFormatee, pourcentage, tachesRestantes, totalTaches
            );
        }

        // En retard
        if (retardJours <= 5) {
            return String.format(
                    "🟠 Le projet \"%s\" accuse un retard de %d jour(s). " +
                            "À votre rythme actuel de %s tâches/jour, la livraison est estimée au %s " +
                            "au lieu du %s prévu. " +
                            "Il reste %d tâche(s) à compléter (%d%% d'avancement). " +
                            "Une accélération du rythme est recommandée.",
                    project.getTitle(), retardJours, velociteFormatee,
                    projeteFormatee, endDateFormatee,
                    tachesRestantes, pourcentage
            );
        }

        // Critique
        return String.format(
                "🔴 ALERTE — Le projet \"%s\" est en retard critique de %d jours ! " +
                        "Vélocité actuelle : %s tâches/jour — livraison projetée au %s " +
                        "alors que la deadline est fixée au %s. " +
                        "%d tâche(s) restantes (%d%% d'avancement seulement). " +
                        "Une réunion de crise et une révision du périmètre sont fortement recommandées.",
                project.getTitle(), retardJours, velociteFormatee,
                projeteFormatee, endDateFormatee,
                tachesRestantes, pourcentage
        );
    }

    /**
     * Retourne un BurndownChartDTO vide si le projet n'a aucune tâche.
     */
    private BurndownChartDTO buildEmptyBurndown(Project project) {
        return BurndownChartDTO.builder()
                .projectId(project.getId())
                .projectTitle(project.getTitle())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .totalTaches(0)
                .courbeIdeale(new ArrayList<>())
                .courbeReelle(new ArrayList<>())
                .tachesRestantesAujourdhui(0)
                .tachesCompletees(0)
                .retardEstimeJours(0)
                .velociteJournaliere(0.0)
                .dateLivraisonProjetee(project.getEndDate())
                .statutBurndown("DANS_LES_DELAIS")
                .analyse(String.format(
                        "ℹ️ Aucune tâche n'a encore été créée pour le projet \"%s\". " +
                                "Ajoutez des tâches pour visualiser le burndown chart.",
                        project.getTitle()))
                .build();
    }
}