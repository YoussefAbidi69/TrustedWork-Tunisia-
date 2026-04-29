package tn.esprit.mscontractservicee.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.dto.DeliveryProofSubmitRequest;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.DeliveryProof;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.enums.DeliveryStatus;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.enums.MilestoneStatus;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.DeliveryProofRepository;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;
import tn.esprit.mscontractservicee.service.calculation.ContractAmountCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MilestoneServiceImpl implements IMilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final DeliveryProofRepository deliveryProofRepository;
    private final IPaymentService paymentService;
    private final ContractRepository contractRepository;
    private final INotificationService notificationService;

    private static final String CONTRACT_NOT_FOUND_MSG = "Contract not found with id: ";
    private static final String MILESTONE_NOT_FOUND_MSG = "Milestone not found with id: ";

    @org.springframework.beans.factory.annotation.Value("${app.frontend.contract-activity-prefix:/app/activity/contracts/}")
    private String contractActivityPathPrefix;

    private Contract requireDraftContractWithBudget(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        CONTRACT_NOT_FOUND_MSG + contractId));
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Milestones can only be modified while contract is DRAFT. Current status: " + contract.getStatus());
        }
        if (contract.getMontantTotal() == null || contract.getMontantTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contract montantTotal (budget) is required before creating milestones");
        }
        return contract;
    }

    private void assertWithinBudget(Contract contract, BigDecimal newTotal) {
        if (newTotal.compareTo(contract.getMontantTotal()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Milestones total exceeds contract budget. budget=" + contract.getMontantTotal()
                            + " milestonesTotal=" + newTotal);
        }
    }

    @Override
    public Milestone createMilestone(Milestone milestone) {
        if (milestone.getContractId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contractId is required");
        }
        log.info("Creating milestone for contract: {}", milestone.getContractId());

        Contract contract = requireDraftContractWithBudget(milestone.getContractId());
        if (milestone.getMontant() == null || milestone.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "milestone.montant is required and must be > 0");
        }

        List<Milestone> existing = milestoneRepository.findByContractIdOrderByOrdreAsc(milestone.getContractId());
        BigDecimal currentTotal;
        try {
            currentTotal = ContractAmountCalculator.computeMilestonesTotal(existing);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
        BigDecimal newTotal = currentTotal.add(milestone.getMontant());
        assertWithinBudget(contract, newTotal);

        milestone.setStatus(MilestoneStatus.PENDING);
        return milestoneRepository.save(milestone);
    }

    @Override
    public Milestone updateMilestone(Long id, Milestone milestone) {
        log.info("Updating milestone with id: {}", id);
        Milestone existing = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));

        if (existing.getStatus() != MilestoneStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PENDING milestones can be updated. Current status: " + existing.getStatus());
        }

        Contract contract = requireDraftContractWithBudget(existing.getContractId());
        if (milestone.getMontant() == null || milestone.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "milestone.montant is required and must be > 0");
        }

        List<Milestone> all = milestoneRepository.findByContractIdOrderByOrdreAsc(existing.getContractId());
        BigDecimal otherTotal = BigDecimal.ZERO;
        for (Milestone m : all) {
            if (m == null || m.getId() == null || m.getId().equals(existing.getId())) {
                continue;
            }
            if (m.getMontant() == null || m.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Milestone montant must be set and > 0. milestoneId=" + m.getId());
            }
            otherTotal = otherTotal.add(m.getMontant());
        }
        BigDecimal newTotal = otherTotal.add(milestone.getMontant());
        assertWithinBudget(contract, newTotal);

        existing.setTitre(milestone.getTitre());
        existing.setDescription(milestone.getDescription());
        existing.setMontant(milestone.getMontant());
        existing.setDeadline(milestone.getDeadline());

        return milestoneRepository.save(existing);
    }

    @Override
    public Optional<Milestone> findById(Long id) {
        return milestoneRepository.findById(id);
    }

    @Override
    public Page<Milestone> findAll(Pageable pageable) {
        return milestoneRepository.findAll(pageable);
    }

    @Override
    public Milestone updateStatus(Long id, MilestoneStatus status) {
        log.info("Updating milestone {} status to: {}", id, status);
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));

        milestone.setStatus(status);
        return milestoneRepository.save(milestone);
    }

    @Override
    public Milestone startMilestone(Long id) {
        log.info("Starting milestone: {}", id);
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));

        if (milestone.getStatus() != MilestoneStatus.PENDING && milestone.getStatus() != MilestoneStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Milestone cannot be started. Current status: " + milestone.getStatus());
        }

        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        milestone.setStartedAt(LocalDateTime.now());

        return milestoneRepository.save(milestone);
    }

    @Override
    public Milestone submitMilestone(Long id) {
        log.info("Submitting milestone: {}", id);
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));

        if (milestone.getStatus() != MilestoneStatus.IN_PROGRESS && milestone.getStatus() != MilestoneStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Milestone cannot be submitted. Current status: " + milestone.getStatus());
        }

        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setSubmittedAt(LocalDateTime.now());
        // New submission supersedes any previous validation decision.
        milestone.setValidatedAt(null);
        milestone.setRejectionReason(null);

        Milestone saved = milestoneRepository.save(milestone);
        
        // Notification au client
        contractRepository.findById(saved.getContractId()).ifPresent(contract -> notificationService.createNotification(
                contract.getClientCin(),
                "Jalon soumis",
                "Le freelancer a soumis le jalon : " + saved.getTitre(),
                tn.esprit.mscontractservicee.enums.NotificationType.INFO,
                contractActivityPathPrefix + contract.getId()
        ));

        return saved;
    }

    @Override
    public Milestone submitMilestoneWithProof(Long id, DeliveryProofSubmitRequest proof) {
        if (proof == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deliveryProof payload is required");
        }

        log.info("Submitting milestone with delivery proof: {}", id);
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));

        if (milestone.getStatus() != MilestoneStatus.IN_PROGRESS && milestone.getStatus() != MilestoneStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Milestone cannot be submitted. Current status: " + milestone.getStatus());
        }

        DeliveryProof deliveryProof = deliveryProofRepository.findByMilestoneId(id)
                .orElseGet(() -> DeliveryProof.builder().milestoneId(id).build());

        deliveryProof.setFichiers(proof.fichiers());
        deliveryProof.setLienDemo(proof.lienDemo());
        deliveryProof.setRepoGit(proof.repoGit());
        deliveryProof.setCommentaire(proof.commentaire());
        deliveryProof.setHashMD5(proof.hashMD5());
        deliveryProof.setSubmittedAt(LocalDateTime.now());
        deliveryProof.setStatus(DeliveryStatus.SUBMITTED);
        deliveryProof.setApprovedAt(null);
        deliveryProof.setApprovedByCin(null);
        deliveryProofRepository.save(deliveryProof);

        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setSubmittedAt(LocalDateTime.now());
        // New submission supersedes any previous validation decision.
        milestone.setValidatedAt(null);
        milestone.setRejectionReason(null);

        Milestone saved = milestoneRepository.save(milestone);
        
        // Notification au client
        contractRepository.findById(saved.getContractId()).ifPresent(contract -> notificationService.createNotification(
                contract.getClientCin(),
                "Preuve de livraison soumise",
                "Le freelancer a soumis le jalon avec des preuves pour : " + saved.getTitre(),
                tn.esprit.mscontractservicee.enums.NotificationType.INFO,
                contractActivityPathPrefix + contract.getId()
        ));

        return saved;
    }

    @Override
    public Milestone approveMilestone(Long id, Long approvedByCin) {
        log.info("Approving milestone: {}", id);
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));

        if (milestone.getStatus() != MilestoneStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Milestone cannot be approved. Current status: " + milestone.getStatus());
        }

        milestone.setStatus(MilestoneStatus.APPROVED);
        milestone.setValidatedAt(LocalDateTime.now());

        deliveryProofRepository.findByMilestoneId(id).ifPresent(proof -> {
            proof.setStatus(DeliveryStatus.APPROVED);
            proof.setApprovedAt(LocalDateTime.now());
            proof.setApprovedByCin(approvedByCin);
            deliveryProofRepository.save(proof);
        });

        Milestone saved = milestoneRepository.save(milestone);
        try {
            paymentService.releaseApprovedMilestone(saved.getId());
            
            // Notification au freelancer
            contractRepository.findById(saved.getContractId()).ifPresent(contract -> notificationService.createNotification(
                    contract.getFreelancerCin(),
                    "Jalon approuvé !",
                    "Le client a approuvé le jalon : " + saved.getTitre() + ". Les fonds sont débloqués.",
                    tn.esprit.mscontractservicee.enums.NotificationType.SUCCESS,
                    contractActivityPathPrefix + contract.getId()
            ));
            
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment release failed: " + e.getMessage(), e);
        }
        return saved;
    }

    @Override
    public Milestone autoApproveMilestone(Long id, Long approvedByCin) {
        log.info("Auto-approving milestone: {}", id);
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));

        if (milestone.getStatus() != MilestoneStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Milestone cannot be auto-approved. Current status: " + milestone.getStatus());
        }

        milestone.setStatus(MilestoneStatus.AUTO_APPROVED);
        milestone.setValidatedAt(LocalDateTime.now());

        deliveryProofRepository.findByMilestoneId(id).ifPresent(proof -> {
            proof.setStatus(DeliveryStatus.APPROVED);
            proof.setApprovedAt(LocalDateTime.now());
            proof.setApprovedByCin(approvedByCin);
            deliveryProofRepository.save(proof);
        });

        Milestone saved = milestoneRepository.save(milestone);
        try {
            paymentService.releaseApprovedMilestone(saved.getId());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment release failed: " + e.getMessage(), e);
        }
        return saved;
    }

    @Override
    public void deleteMilestone(Long id) {
        log.info("Deleting milestone with id: {}", id);
        milestoneRepository.deleteById(id);
    }

    @Override
    public Milestone rejectMilestone(Long id, String rejectionReason, LocalDate newDeadline) {
        log.info("Rejecting milestone: {} with reason: {}", id, rejectionReason);

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rejectionReason is required");
        }
        if (newDeadline != null && newDeadline.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "newDeadline must be today or in the future");
        }

        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));

        if (milestone.getStatus() != MilestoneStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Milestone cannot be rejected. Current status: " + milestone.getStatus());
        }

        milestone.setStatus(MilestoneStatus.REJECTED);
        milestone.setRejectionReason(rejectionReason.trim());
        milestone.setValidatedAt(LocalDateTime.now());
        if (newDeadline != null) {
            milestone.setDeadline(newDeadline);
        }

        deliveryProofRepository.findByMilestoneId(id).ifPresent(proof -> {
            proof.setStatus(DeliveryStatus.REJECTED);
            proof.setApprovedAt(null);
            proof.setApprovedByCin(null);
            deliveryProofRepository.save(proof);
        });

        Milestone saved = milestoneRepository.save(milestone);
        
        // Notification au freelancer
        contractRepository.findById(saved.getContractId()).ifPresent(contract -> 
            notificationService.createNotification(
                contract.getFreelancerCin(),
                "Jalon rejeté / Révision demandée",
                "Le client a demandé des modifications sur le jalon : " + saved.getTitre() + ". Motif: " + rejectionReason,
                tn.esprit.mscontractservicee.enums.NotificationType.WARNING,
                contractActivityPathPrefix + contract.getId()
            )
        );

        return saved;
    }

    @Override
    public Milestone updateRejectedMilestoneDeadline(Long id, LocalDate newDeadline) {
        log.info("Updating rejected milestone: {} deadline to: {}", id, newDeadline);

        if (newDeadline == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "newDeadline is required");
        }
        if (newDeadline.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "newDeadline must be today or in the future");
        }

        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MILESTONE_NOT_FOUND_MSG + id));

        if (milestone.getStatus() != MilestoneStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Deadline can only be changed for REJECTED milestones. Current status: " + milestone.getStatus());
        }

        milestone.setDeadline(newDeadline);
        return milestoneRepository.save(milestone);
    }

    @Override
    // Backward-compatible overload (e.g. if some code calls the impl directly).
    public Milestone rejectMilestone(Long id, String rejectionReason) {
        return rejectMilestone(id, rejectionReason, null);
    }

    @Override
    public List<Milestone> findByContractId(Long contractId) {
        log.info("Finding milestones for contract: {}", contractId);
        return milestoneRepository.findByContractIdOrderByOrdreAsc(contractId);
    }

    @Override
    public List<Milestone> findForClientCin(Long clientCin) {
        if (clientCin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientCin is required");
        }
        log.info("Finding milestones for client: {}", clientCin);
        return milestoneRepository.findForClient(clientCin);
    }

    @Override
    public List<Milestone> findForSignedFreelancerCin(Long freelancerCin) {
        if (freelancerCin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "freelancerCin is required");
        }
        log.info("Finding milestones for signed freelancer contracts: {}", freelancerCin);
        return milestoneRepository.findForSignedFreelancer(freelancerCin);
    }
}
