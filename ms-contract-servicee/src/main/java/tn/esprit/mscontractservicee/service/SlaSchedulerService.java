package tn.esprit.mscontractservicee.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.enums.MilestoneStatus;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;

import tn.esprit.mscontractservicee.dto.UserDTO;
import tn.esprit.mscontractservicee.feign.UserServiceClient;
import tn.esprit.mscontractservicee.service.email.AppEmailService;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import tn.esprit.mscontractservicee.enums.EscrowStatus;
import tn.esprit.mscontractservicee.repository.EscrowAccountRepository;
import tn.esprit.mscontractservicee.entity.EscrowAccount;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlaSchedulerService {

    private final MilestoneRepository milestoneRepository;
    private final IMilestoneService milestoneService;
    private final IPaymentService paymentService;
    private final AppEmailService emailService;
    private final UserServiceClient userServiceClient;
    private final EscrowAccountRepository escrowAccountRepository;

    /**
     * Pilier 1: Protection du Client (SLA Freelancer).
     * S'exécute toutes les heures.
     * Vérifie si le freelancer a dépassé la deadline + slaFreelancerHeures.
     */
    //@Scheduled(fixedRate = 10000) // Exécute toutes les 10 secondes
   // @Scheduled(cron = "0 * * * * *")
    @Scheduled(cron = "0 0 * * * ?") // Toutes les heures pile
    public void checkFreelancerDeadlines() {
        log.info("Lancement du Job : Vérification des retards Freelancers (SLA Freelancer)");

        // 1. Récupérer les jalons en cours d'exécution
        List<Milestone> inProgressMilestones = milestoneRepository.findByStatus(MilestoneStatus.IN_PROGRESS);

        for (Milestone milestone : inProgressMilestones) {
            Contract contract = milestone.getContract();

            // Vérifier si le contrat est bloqué par un litige (Escrow DISPUTED)
            EscrowAccount escrow = escrowAccountRepository.findByContractId(contract.getId()).orElse(null);
            if (escrow != null && escrow.getStatus() == EscrowStatus.DISPUTED) {
                log.info("SLA ignoré pour le jalon {} : Le contrat est en litige (Escrow bloqué).", milestone.getId());
                continue; // On passe au jalon suivant
            }

            // Délai de grâce : par défaut 48h si non spécifié
            int slaHeures = (contract.getSlaFreelancerHeures() != null && contract.getSlaFreelancerHeures() > 0)
                    ? contract.getSlaFreelancerHeures()
                    : 48;

            if (milestone.getDeadline() != null) {
                // On calcule la limite absolue : (Deadline à 23h59:59) + slaHeures
                LocalDateTime absoluteLimit = milestone.getDeadline()
                        .atTime(LocalTime.MAX)
                        .plusHours(slaHeures);

                if (LocalDateTime.now().isAfter(absoluteLimit)) {
                    log.warn("SLA dépassé pour le jalon ID {}. Annulation et remboursement.", milestone.getId());

                    try {
                        // 2. Annulation
                        milestone.setStatus(MilestoneStatus.CANCELLED);
                        milestone.setRejectionReason(
                                "SLA dépassé : Non soumis dans les temps (" + slaHeures + "h après deadline).");
                        milestoneRepository.save(milestone);

                        // 3. Remboursement du client depuis l'Escrow
                        paymentService.refundMilestoneToClient(milestone.getId());

                    } catch (Exception e) {
                        log.error("Erreur lors de l'annulation du jalon " + milestone.getId(), e);
                    }
                }
            }
        }
    }

    /**
     * Pilier 2: Protection du Freelancer (SLA Client).
     * S'exécute tous les jours à minuit.
     * Vérifie si le client a oublié de valider le jalon après slaClientJours.
     */
   // @Scheduled(fixedRate = 10000) // Exécute toutes les 10 secondes
   // @Scheduled(cron = "0 * * * * *")
    @Scheduled(cron = "0 0 0 * * ?") // Tous les jours à minuit
    public void checkClientApprovals() {
        log.info("Lancement du Job : Vérification des approbations Clients (SLA Client)");

        // 1. Récupérer les jalons soumis
        List<Milestone> submittedMilestones = milestoneRepository.findByStatus(MilestoneStatus.SUBMITTED);

        for (Milestone milestone : submittedMilestones) {
            Contract contract = milestone.getContract();

            // Vérifier si le contrat est bloqué par un litige (Escrow DISPUTED)
            EscrowAccount escrow = escrowAccountRepository.findByContractId(contract.getId()).orElse(null);
            if (escrow != null && escrow.getStatus() == EscrowStatus.DISPUTED) {
                log.info("SLA ignoré pour le jalon {} : Le contrat est en litige (Escrow bloqué).", milestone.getId());
                continue; // On passe au jalon suivant
            }

            // Délai d'inspection : par défaut 7 jours si non spécifié
            int slaJours = (contract.getSlaClientJours() != null && contract.getSlaClientJours() > 0)
                    ? contract.getSlaClientJours()
                    : 7;

            if (milestone.getSubmittedAt() != null) {
                // Limite absolue pour que le client approuve
                LocalDateTime approvalLimit = milestone.getSubmittedAt().plusDays(slaJours);
              // LocalDateTime approvalLimit = milestone.getSubmittedAt().plusMinutes(2);

                if (LocalDateTime.now().isAfter(approvalLimit)) {
                    log.warn("SLA Client dépassé pour le jalon ID {}. Auto-approbation.", milestone.getId());

                    try {
                        // 2. Auto-Approbation (Passe le statut et transfère l'argent au freelancer)
                        // L'ID 0L (ou autre convention) indique que c'est le système qui approuve.
                        milestoneService.autoApproveMilestone(milestone.getId(), 0L);
                    } catch (Exception e) {
                        log.error("Erreur lors de l'auto-approbation du jalon " + milestone.getId(), e);
                    }
                } else {
                    // Vérifier si c'est exactement le jour précédent (24h avant) pour envoyer l'email
                    long hoursLeft = ChronoUnit.HOURS.between(LocalDateTime.now(), approvalLimit);
                    if (hoursLeft > 0 && hoursLeft <= 24) {
                  /*  long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), approvalLimit);
                    if (minutesLeft > 0 && minutesLeft <= 1) {*/
                        log.info("Envoi d'un email d'avertissement au client pour le jalon ID {}", milestone.getId());
                        try {
                            UserDTO client = userServiceClient.getPublicUserByCin(contract.getClientCin());
                            if (client != null && client.getEmail() != null) {
                                String subject = "URGENT : Approbation automatique demain";
                                String body = "Bonjour,\n\n" +
                                        "Le freelancer a soumis le jalon '" + milestone.getTitre() + "' depuis "
                                        + (slaJours - 1) + " jours.\n" +
                                        "Si vous ne validez pas ce travail ou ne demandez pas de révision aujourd'hui, les fonds seront automatiquement transférés au freelancer demain.\n\n"
                                        +
                                        "Merci de vous connecter pour valider le travail.\n\n" +
                                        "L'équipe TrustedWork.";
                                emailService.sendSimpleEmail(client.getEmail(), subject, body);
                            }
                        } catch (Exception e) {
                            log.error("Impossible d'envoyer l'email d'avertissement pour le jalon " + milestone.getId(),
                                    e);
                        }
                    }
                }
            }
        }
    }
}
