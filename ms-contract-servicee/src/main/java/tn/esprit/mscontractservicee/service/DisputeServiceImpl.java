package tn.esprit.mscontractservicee.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.dto.dispute.DisputeAssignRequest;
import tn.esprit.mscontractservicee.dto.dispute.DisputeCreateRequest;
import tn.esprit.mscontractservicee.dto.dispute.DisputeResolveRequest;
import tn.esprit.mscontractservicee.dto.dispute.DisputeRespondRequest;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Dispute;
import tn.esprit.mscontractservicee.entity.EscrowAccount;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.entity.Transaction;
import tn.esprit.mscontractservicee.entity.Wallet;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.enums.DisputeStatus;
import tn.esprit.mscontractservicee.enums.EscrowStatus;
import tn.esprit.mscontractservicee.enums.MilestoneStatus;
import tn.esprit.mscontractservicee.enums.PaymentMethod;
import tn.esprit.mscontractservicee.enums.TransactionStatus;
import tn.esprit.mscontractservicee.enums.TransactionType;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.DisputeRepository;
import tn.esprit.mscontractservicee.repository.EscrowAccountRepository;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;
import tn.esprit.mscontractservicee.repository.TransactionRepository;
import tn.esprit.mscontractservicee.repository.WalletRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DisputeServiceImpl implements IDisputeService {

    private static final Set<DisputeStatus> OPEN_STATUSES = Set.of(
            DisputeStatus.OPEN,
            DisputeStatus.RESPONDED,
            DisputeStatus.UNDER_REVIEW
    );
    private static final Set<DisputeStatus> FINAL_STATUSES = Set.of(
            DisputeStatus.RESOLVED_CLIENT,
            DisputeStatus.RESOLVED_FREELANCER,
            DisputeStatus.SPLIT,
            DisputeStatus.DISMISSED
    );

    private final DisputeRepository disputeRepository;
    private final ContractRepository contractRepository;
    private final MilestoneRepository milestoneRepository;
    private final EscrowAccountRepository escrowAccountRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final IWalletService walletService;
    private final INotificationService notificationService;

    // Wallet de la plateforme (commission). Par défaut: wallet.id=1
    @Value("${platform.wallet.id:1}")
    private Long platformWalletId;

    // Frontend deep-link for disputes (override via config if routes change)
    @Value("${app.frontend.paths.disputeActivity:/app/activity/disputes}")
    private String disputeActivityPath;

    @Override
    public Dispute openDispute(Long authenticatedCin, DisputeCreateRequest request) {
        validateOpenDisputeRequest(authenticatedCin, request);

        Contract contract = requireContract(request.getContractId());
        requireParticipant(contract, authenticatedCin);

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dispute can only be opened for ACTIVE contracts. Status: " + contract.getStatus());
        }

        Milestone milestone = validateDisputeUniquenessAndMilestone(contract, request);

        EscrowAccount escrow = escrowAccountRepository.findByContractId(contract.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Escrow not found for contract. Contract must be paid/active first."));

        // Freeze escrow while the dispute is open.
        escrow.setStatus(EscrowStatus.DISPUTED);
        escrow.setUpdatedAt(LocalDateTime.now());
        escrowAccountRepository.save(escrow);

        Long defendantCin = resolveDefendantCin(contract, authenticatedCin);

        Dispute dispute = Dispute.builder()
                .reference("DSP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .contractId(contract.getId())
                .milestoneId(milestone != null ? milestone.getId() : null)
                // Note: fields are named *Id* but in this service we store CIN values.
                .plaignantId(authenticatedCin)
                .defendantId(defendantCin)
                .motif(request.getMotif().trim())
                .preuvesPlaignant(request.getPreuvesPlaignant())
                .openedAt(LocalDateTime.now())
                .status(DisputeStatus.OPEN)
                .build();

        Dispute saved = disputeRepository.save(dispute);
        
        // Notification au défendeur
        notificationService.createNotification(
            defendantCin,
            "Litige ouvert",
            "Un litige a été ouvert sur le contrat #" + contract.getId() + ". Veuillez y répondre.",
            tn.esprit.mscontractservicee.enums.NotificationType.URGENT,
            disputeActivityPath
        );
        
        // Notification aux administrateurs (identifié par 0)
        notificationService.createNotification(
            0L,
            "Nouveau litige détecté",
            "Le contrat #" + contract.getId() + " a un nouveau litige ouvert.",
            tn.esprit.mscontractservicee.enums.NotificationType.WARNING,
            "/admin/activity/disputes"
        );

        return saved;
    }

    @Override
    public Dispute respond(Long disputeId, Long authenticatedCin, boolean admin, DisputeRespondRequest request) {
        if (authenticatedCin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (request == null || request.getPreuvesDefense() == null || request.getPreuvesDefense().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "preuvesDefense is required");
        }

        Dispute dispute = requireDispute(disputeId);
        if (FINAL_STATUSES.contains(dispute.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dispute is already closed");
        }
        if (dispute.getStatus() != DisputeStatus.OPEN && dispute.getStatus() != DisputeStatus.RESPONDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dispute cannot be responded to. Status: " + dispute.getStatus());
        }

        Contract contract = requireContract(dispute.getContractId());
        requireParticipant(contract, authenticatedCin);

        if (!admin && (dispute.getDefendantId() == null || !dispute.getDefendantId().equals(authenticatedCin))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the defendant can respond to this dispute");
        }

        dispute.setPreuvesDefense(request.getPreuvesDefense().trim());
        // Mark that the defendant answered (helps frontend follow workflow before admin assignment).
        dispute.setStatus(DisputeStatus.RESPONDED);
        
        Dispute saved = disputeRepository.save(dispute);
        
        // Notification au plaignant
        notificationService.createNotification(
            dispute.getPlaignantId(),
            "Réponse au litige",
            "L'autre partie a répondu au litige sur le contrat #" + contract.getId() + ".",
            tn.esprit.mscontractservicee.enums.NotificationType.INFO,
            disputeActivityPath
        );
        
        return saved;
    }

    @Override
    public Dispute assign(Long disputeId, Long adminCin, DisputeAssignRequest request) {
        if (adminCin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Dispute dispute = requireDispute(disputeId);
        if (dispute.getStatus() != DisputeStatus.OPEN && dispute.getStatus() != DisputeStatus.RESPONDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dispute cannot be assigned. Status: " + dispute.getStatus());
        }

        Long arbitreCin = request != null ? request.getArbitreId() : null;
        if (arbitreCin == null) {
            arbitreCin = adminCin;
        }

        dispute.setArbitreId(arbitreCin);
        dispute.setAssignedAt(LocalDateTime.now());
        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        return disputeRepository.save(dispute);
    }

    @Override
    public Dispute resolve(Long disputeId, Long adminCin, DisputeResolveRequest request) {
        requireAdmin(adminCin);
        requireResolveRequest(request);

        Dispute dispute = requireDisputeForResolution(disputeId);
        Contract contract = requireContract(dispute.getContractId());
        EscrowAccount escrow = requireEscrow(contract.getId());
        Milestone milestone = dispute.getMilestoneId() != null ? requireMilestone(dispute.getMilestoneId()) : null;

        DisputeStatus target = requireValidResolutionStatus(request.getStatus());
        BigDecimal refund = nvl(request.getMontantRembourse());
        BigDecimal release = nvl(request.getMontantLibere());
        BigDecimal sum = refund.add(release);

        if (target == DisputeStatus.DISMISSED) {
            return dismissDispute(dispute, escrow, request);
        }

        validateDisputeAmounts(refund, release, sum, milestone, escrow, target);
        applyMoneyMovements(contract, dispute.getMilestoneId(), escrow, refund, release);
        updateMilestoneAfterResolution(milestone, release);
        restoreEscrowAndUpdateContract(contract, escrow);

        Dispute saved = persistResolution(dispute, request, refund, release, target);
        notifyPartiesAfterResolution(contract, target);
        return saved;
    }

    private static void requireAdmin(Long adminCin) {
        if (adminCin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    private static void requireResolveRequest(DisputeResolveRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
    }

    private Dispute requireDisputeForResolution(Long disputeId) {
        Dispute dispute = requireDispute(disputeId);
        if (FINAL_STATUSES.contains(dispute.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dispute is already closed");
        }
        if (dispute.getStatus() != DisputeStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dispute must be UNDER_REVIEW to resolve. Status: " + dispute.getStatus());
        }
        return dispute;
    }

    private EscrowAccount requireEscrow(Long contractId) {
        return escrowAccountRepository.findByContractId(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escrow not found"));
    }

    private static DisputeStatus requireValidResolutionStatus(DisputeStatus target) {
        if (target == DisputeStatus.OPEN || target == DisputeStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid resolution status: " + target);
        }
        return target;
    }

    private Dispute dismissDispute(Dispute dispute, EscrowAccount escrow, DisputeResolveRequest request) {
        if (request.getDecision() == null || request.getDecision().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "decision is required when dismissing a dispute");
        }

        // Unfreeze escrow; no money movement.
        restoreEscrowStatusAfterResolution(escrow);
        escrow.setUpdatedAt(LocalDateTime.now());
        escrowAccountRepository.save(escrow);

        dispute.setDecision(request.getDecision().trim());
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setStatus(DisputeStatus.DISMISSED);
        return disputeRepository.save(dispute);
    }

    private void applyMoneyMovements(Contract contract, Long milestoneId, EscrowAccount escrow, BigDecimal refund, BigDecimal release) {
        if (refund.compareTo(BigDecimal.ZERO) > 0) {
            doRefund(contract, milestoneId, escrow, refund);
        }
        if (release.compareTo(BigDecimal.ZERO) > 0) {
            doRelease(contract, milestoneId, escrow, release);
        }
    }

    private void updateMilestoneAfterResolution(Milestone milestone, BigDecimal release) {
        if (milestone == null) {
            return;
        }
        milestone.setValidatedAt(LocalDateTime.now());
        if (release.compareTo(BigDecimal.ZERO) > 0) {
            milestone.setStatus(MilestoneStatus.APPROVED);
        } else {
            milestone.setStatus(MilestoneStatus.REJECTED);
            milestone.setRejectionReason("Dispute resolved in favor of client");
        }
        milestoneRepository.save(milestone);
    }

    private void restoreEscrowAndUpdateContract(Contract contract, EscrowAccount escrow) {
        // Unfreeze escrow and potentially close contract.
        restoreEscrowStatusAfterResolution(escrow);
        escrow.setUpdatedAt(LocalDateTime.now());
        escrowAccountRepository.save(escrow);

        if (escrow.getStatus() == EscrowStatus.RELEASED) {
            contract.setStatus(ContractStatus.COMPLETED);
            contract.setUpdatedAt(LocalDateTime.now());
            contractRepository.save(contract);
        } else if (escrow.getStatus() == EscrowStatus.REFUNDED) {
            contract.setStatus(ContractStatus.TERMINATED);
            contract.setUpdatedAt(LocalDateTime.now());
            contractRepository.save(contract);
        }
    }

    private Dispute persistResolution(Dispute dispute, DisputeResolveRequest request, BigDecimal refund, BigDecimal release, DisputeStatus target) {
        dispute.setDecision(request.getDecision() != null ? request.getDecision().trim() : null);
        dispute.setMontantRembourse(refund.compareTo(BigDecimal.ZERO) > 0 ? refund : null);
        dispute.setMontantLibere(release.compareTo(BigDecimal.ZERO) > 0 ? release : null);
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setStatus(target);
        return disputeRepository.save(dispute);
    }

    private void notifyPartiesAfterResolution(Contract contract, DisputeStatus target) {
        String notifMsg = "Le litige sur le contrat #" + contract.getId() + " a été résolu. Statut: " + target;
        notificationService.createNotification(
                contract.getClientCin(), "Litige résolu", notifMsg,
                tn.esprit.mscontractservicee.enums.NotificationType.INFO, disputeActivityPath
        );
        notificationService.createNotification(
                contract.getFreelancerCin(), "Litige résolu", notifMsg,
                tn.esprit.mscontractservicee.enums.NotificationType.INFO, disputeActivityPath
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Dispute getByIdForUser(Long disputeId, Long authenticatedCin, boolean admin) {
        if (authenticatedCin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Dispute dispute = requireDispute(disputeId);
        Contract contract = requireContract(dispute.getContractId());
        if (!admin) {
            requireParticipant(contract, authenticatedCin);
        }
        return dispute;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dispute> listByContractForUser(Long contractId, Long authenticatedCin, boolean admin) {
        if (authenticatedCin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (contractId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contractId is required");
        }
        Contract contract = requireContract(contractId);
        if (!admin) {
            requireParticipant(contract, authenticatedCin);
        }
        return disputeRepository.findByContractIdOrderByOpenedAtDesc(contractId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dispute> listByMilestoneForUser(Long milestoneId, Long authenticatedCin, boolean admin) {
        if (authenticatedCin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (milestoneId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "milestoneId is required");
        }

        Milestone milestone = requireMilestone(milestoneId);
        Contract contract = requireContract(milestone.getContractId());
        if (!admin) {
            requireParticipant(contract, authenticatedCin);
        }

        return disputeRepository.findByMilestoneIdOrderByOpenedAtDesc(milestoneId);
    }

    private void doRefund(Contract contract, Long milestoneId, EscrowAccount escrow, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        // Update escrow balance first (logical funds movement)
        escrow.setMontantBloque(escrow.getMontantBloque().subtract(amount));
        escrow.setRefundedAt(LocalDateTime.now());

        String scope = milestoneId != null ? (" milestone #" + milestoneId) : "";
        walletService.credit(contract.getClientCin(), amount,
                "Refund dispute for contract #" + contract.getId() + scope);

        Wallet clientWallet = walletService.getOrCreateWallet(contract.getClientCin());
        Transaction tx = Transaction.builder()
                .reference("TRX-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .contractId(contract.getId())
                .milestoneId(milestoneId)
                .escrowId(escrow.getId())
                .walletId(clientWallet.getId())
                .type(TransactionType.REFUND)
                .montant(amount)
                .commissionDynamique(BigDecimal.ZERO)
                .montantCommission(BigDecimal.ZERO)
                .montantNet(amount)
                .methodePaiement(PaymentMethod.WALLET)
                .description("Dispute refund to client")
                .status(TransactionStatus.PROCESSED)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);
    }

    private void doRelease(Contract contract, Long milestoneId, EscrowAccount escrow, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal commissionRate = contract.getCommissionRate() != null ? contract.getCommissionRate() : BigDecimal.valueOf(10);
        BigDecimal commission = amount.multiply(commissionRate).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal netAmount = amount.subtract(commission);

        if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Net amount cannot be negative. amount=" + amount + " commission=" + commission);
        }

        escrow.setMontantBloque(escrow.getMontantBloque().subtract(amount));
        escrow.setMontantLibere(escrow.getMontantLibere().add(amount));
        escrow.setReleasedAt(LocalDateTime.now());

        String scope = milestoneId != null ? (" milestone #" + milestoneId) : "";
        walletService.credit(contract.getFreelancerCin(), netAmount,
                "Dispute release for contract #" + contract.getId() + scope);

        // Créditer la plateforme (commission) - wallet configurable, par défaut id=1
        if (commission.compareTo(BigDecimal.ZERO) > 0) {
            Wallet platformWallet = walletRepository.findById(platformWalletId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Platform wallet not found. walletId=" + platformWalletId));

            walletService.credit(platformWallet.getUserCin(), commission,
                    "Commission (dispute release) for contract #" + contract.getId() + scope);

            Transaction commissionTx = Transaction.builder()
                    .reference("TRX-COM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .contractId(contract.getId())
                    .milestoneId(milestoneId)
                    .escrowId(escrow.getId())
                    .walletId(platformWallet.getId())
                    .type(TransactionType.COMMISSION)
                    .montant(commission)
                    .commissionDynamique(commissionRate)
                    .montantCommission(commission)
                    .montantNet(commission)
                    .methodePaiement(PaymentMethod.WALLET)
                    .description("Commission plateforme (dispute)")
                    .status(TransactionStatus.PROCESSED)
                    .createdAt(LocalDateTime.now())
                    .processedAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(commissionTx);
        }

        Wallet freelancerWallet = walletService.getOrCreateWallet(contract.getFreelancerCin());
        Transaction tx = Transaction.builder()
                .reference("TRX-REL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .contractId(contract.getId())
                .milestoneId(milestoneId)
                .escrowId(escrow.getId())
                .walletId(freelancerWallet.getId())
                .type(TransactionType.RELEASE)
                .montant(amount)
                .commissionDynamique(commissionRate)
                .montantCommission(commission)
                .montantNet(netAmount)
                .methodePaiement(PaymentMethod.WALLET)
                .description("Dispute release to freelancer")
                .status(TransactionStatus.PROCESSED)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private void restoreEscrowStatusAfterResolution(EscrowAccount escrow) {
        if (escrow.getMontantBloque() == null) {
            escrow.setMontantBloque(BigDecimal.ZERO);
        }
        if (escrow.getMontantLibere() == null) {
            escrow.setMontantLibere(BigDecimal.ZERO);
        }

        if (escrow.getMontantBloque().compareTo(BigDecimal.ZERO) == 0) {
            if (escrow.getMontantLibere().compareTo(BigDecimal.ZERO) == 0) {
                escrow.setStatus(EscrowStatus.REFUNDED);
            } else {
                escrow.setStatus(EscrowStatus.RELEASED);
            }
        } else {
            if (escrow.getMontantLibere().compareTo(BigDecimal.ZERO) == 0) {
                escrow.setStatus(EscrowStatus.LOCKED);
            } else {
                escrow.setStatus(EscrowStatus.PARTIALLY_RELEASED);
            }
        }
    }

    private Dispute requireDispute(Long disputeId) {
        if (disputeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "disputeId is required");
        }
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Dispute not found with id: " + disputeId));
    }

    private Contract requireContract(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + contractId));
    }

    private Milestone requireMilestone(Long milestoneId) {
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Milestone not found with id: " + milestoneId));
    }

    private void requireParticipant(Contract contract, Long cin) {
        if (contract == null || cin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid contract or user");
        }
        boolean participant = (contract.getClientCin() != null && contract.getClientCin().equals(cin))
                || (contract.getFreelancerCin() != null && contract.getFreelancerCin().equals(cin));
        if (!participant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant of this contract");
        }
    }

    private Long resolveDefendantCin(Contract contract, Long plaignantCin) {
        if (contract.getClientCin() != null && contract.getClientCin().equals(plaignantCin)) {
            return contract.getFreelancerCin();
        }
        if (contract.getFreelancerCin() != null && contract.getFreelancerCin().equals(plaignantCin)) {
            return contract.getClientCin();
        }
        return null;
    }

    private void validateOpenDisputeRequest(Long authenticatedCin, DisputeCreateRequest request) {
        if (authenticatedCin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (request.getContractId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contractId is required");
        }
        if (request.getMotif() == null || request.getMotif().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "motif is required");
        }
    }

    private Milestone validateDisputeUniquenessAndMilestone(Contract contract, DisputeCreateRequest request) {
        boolean contractLevel = request.getMilestoneId() == null;

        // Since escrow is contract-level, allow only one open dispute per contract at a time.
        if (disputeRepository.existsByContractIdAndStatusIn(contract.getId(), OPEN_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "There is already an open dispute for this contract");
        }

        Milestone milestone = null;
        if (!contractLevel) {
            milestone = requireMilestone(request.getMilestoneId());
            if (!milestone.getContractId().equals(contract.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone does not belong to this contract");
            }
            if (milestone.getStatus() != MilestoneStatus.SUBMITTED && milestone.getStatus() != MilestoneStatus.REJECTED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Dispute can only be opened for SUBMITTED/REJECTED milestones. Status: " + milestone.getStatus());
            }

            if (disputeRepository.existsByMilestoneIdAndStatusIn(milestone.getId(), OPEN_STATUSES)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "There is already an open dispute for this milestone");
            }
        } else {
            // Defensive: keep the more specific check available for contract-level disputes.
            if (disputeRepository.existsByContractIdAndMilestoneIdIsNullAndStatusIn(contract.getId(), OPEN_STATUSES)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "There is already an open contract-level dispute for this contract");
            }
        }
        return milestone;
    }

    private void validateDisputeAmounts(BigDecimal refund, BigDecimal release, BigDecimal sum, Milestone milestone, EscrowAccount escrow, DisputeStatus target) {
        validateNonNegativeAndSum(refund, release, sum);
        validateAgainstMilestoneAndEscrow(sum, milestone, escrow);
        validateAmountsForTarget(refund, release, target);
    }

    private static void validateNonNegativeAndSum(BigDecimal refund, BigDecimal release, BigDecimal sum) {
        if (refund.compareTo(BigDecimal.ZERO) < 0 || release.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amounts must be >= 0");
        }
        if (sum.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one amount must be > 0");
        }
    }

    private static void validateAgainstMilestoneAndEscrow(BigDecimal sum, Milestone milestone, EscrowAccount escrow) {
        if (milestone != null && milestone.getMontant() != null && sum.compareTo(milestone.getMontant()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "refund+release exceeds milestone amount. milestone=" + milestone.getMontant() + " provided=" + sum);
        }
        if (escrow.getMontantBloque() == null || escrow.getMontantBloque().compareTo(sum) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient escrow balance. balance=" + escrow.getMontantBloque() + " required=" + sum);
        }
    }

    private static void validateAmountsForTarget(BigDecimal refund, BigDecimal release, DisputeStatus target) {
        if (target == null) {
            return;
        }
        switch (target) {
            case RESOLVED_CLIENT -> {
                if (release.compareTo(BigDecimal.ZERO) != 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "montantLibere must be 0 for RESOLVED_CLIENT");
                }
            }
            case RESOLVED_FREELANCER -> {
                if (refund.compareTo(BigDecimal.ZERO) != 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "montantRembourse must be 0 for RESOLVED_FREELANCER");
                }
            }
            case SPLIT -> {
                if (refund.compareTo(BigDecimal.ZERO) == 0 || release.compareTo(BigDecimal.ZERO) == 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both amounts must be > 0 for SPLIT");
                }
            }
            default -> {
                // no-op
            }
        }
    }
}
