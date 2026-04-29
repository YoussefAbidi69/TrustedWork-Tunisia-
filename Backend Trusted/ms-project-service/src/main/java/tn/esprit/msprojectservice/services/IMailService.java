package tn.esprit.msprojectservice.services;

import tn.esprit.msprojectservice.dto.ProgressReportDTO;
import tn.esprit.msprojectservice.entities.Deliverable;
import tn.esprit.msprojectservice.entities.Project;

/**
 * Service de mailing -- Module 08 TrustedWork Tunisia
 * Phase 2 : emails resolus via UserServiceClient -> Module 01.
 */
public interface IMailService {

    /**
     * Envoie le rapport hebdomadaire au client et au freelancer.
     * Les emails sont resolus en interne via UserServiceClient.getUserById().
     */
    void envoyerRapportHebdomadaire(Project project, ProgressReportDTO report);

    /**
     * Notifie le freelancer que son livrable a ete approuve ou rejete.
     * L'email du freelancer est recupere via le freelancerId du projet lie.
     */
    void envoyerNotificationReviewLivrable(Deliverable deliverable);
}
