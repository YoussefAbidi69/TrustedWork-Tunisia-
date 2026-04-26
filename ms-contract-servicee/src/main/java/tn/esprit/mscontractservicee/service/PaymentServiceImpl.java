package tn.esprit.mscontractservicee.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.mscontractservicee.dto.PaymentIntentResponse;
import tn.esprit.mscontractservicee.entity.*;
import tn.esprit.mscontractservicee.enums.*;
import tn.esprit.mscontractservicee.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements IPaymentService {

    private final ContractRepository contractRepository;
    private final MilestoneRepository milestoneRepository;
    private final EscrowAccountRepository escrowAccountRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final IWalletService walletService;
    private final IStripeService stripeService;
    private final ContractTotalService contractTotalService;

    @Value("${payment.simulation.enabled:false}")
    private boolean simulationEnabled;

    @Value("${app.signature.required:false}")
    private boolean signatureRequired;

    // Wallet de la plateforme (commission). Par défaut: wallet.id=1
    @Value("${platform.wallet.id:1}")
    private Long platformWalletId;

    private static final String CONTRACT_NOT_FOUND_MSG = "Contract not found";
    private static final String MILESTONE_NOT_FOUND_MSG = "Milestone not found";
    private static final String SIMULATION_PREFIX = "🔧 SIMULATION: ";

    @Override
    public PaymentIntentResponse createPaymentIntent(Long contractId, String email) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, CONTRACT_NOT_FOUND_MSG));

        // Safety: prevent charging a contract whose stored total differs from milestones sum.
        contractTotalService.assertStoredTotalMatchesMilestones(contract);
        assertContractPayable(contract);

        if (simulationEnabled) {
            log.info("{}Creating simulated payment intent for contract: {}", SIMULATION_PREFIX, contractId);

            // En simulation, on crée un ID simulé
            String simulatedPaymentId = "sim_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String simulatedClientSecret = "sim_secret_" + UUID.randomUUID().toString().substring(0, 16).toUpperCase();

            return PaymentIntentResponse.builder()
                    .clientSecret(simulatedClientSecret)
                    .paymentIntentId(simulatedPaymentId)
                    .build();
        }

        try {
            var paymentIntent = stripeService.createPaymentIntent(
                    contractId,
                    contract.getMontantTotal(),
                    "usd",
                    email
            );

            return PaymentIntentResponse.builder()
                    .clientSecret(paymentIntent.getClientSecret())
                    .paymentIntentId(paymentIntent.getId())
                    .build();
        } catch (com.stripe.exception.StripeException e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Stripe error: " + e.getMessage());
        }
    }

    private void assertContractPayable(Contract contract) {
        if (signatureRequired) {
            if (contract.getStatus() != ContractStatus.PENDING_PAYMENT) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Contract cannot be paid until it is signed. Status: " + contract.getStatus());
            }
            return;
        }

        if (contract.getStatus() != ContractStatus.DRAFT && contract.getStatus() != ContractStatus.PENDING_PAYMENT) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Contract cannot be paid. Status: " + contract.getStatus());
        }
    }

    private void verifyStripePaymentSucceeded(String paymentIntentId) {
        try {
            var paymentIntent = stripeService.getPaymentIntent(paymentIntentId);
            if (!"succeeded".equals(paymentIntent.getStatus())) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Payment not succeeded: " + paymentIntent.getStatus());
            }
        } catch (com.stripe.exception.StripeException e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Stripe error: " + e.getMessage());
        }
    }

    @Override
    public void confirmPayment(String paymentIntentId, Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, CONTRACT_NOT_FOUND_MSG));

        if (simulationEnabled) {
            log.info("{}Confirming payment for contract: {}", SIMULATION_PREFIX, contractId);
            log.info("   Payment Intent ID: {}", paymentIntentId);
        } else {
            verifyStripePaymentSucceeded(paymentIntentId);
        }

        contractTotalService.assertStoredTotalMatchesMilestones(contract);
        assertContractPayable(contract);

        if (escrowAccountRepository.findByContractId(contractId).isPresent()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Escrow already exists for contract: " + contractId);
        }

        walletService.debit(contract.getClientCin(), contract.getMontantTotal(),
                (simulationEnabled ? SIMULATION_PREFIX : "") + "Paiement contrat #" + contractId);

        EscrowAccount escrow = EscrowAccount.builder()
                .contractId(contract.getId())
                .montantBloque(contract.getMontantTotal())
                .montantLibere(BigDecimal.ZERO)
                .montantTotal(contract.getMontantTotal())
                .status(EscrowStatus.LOCKED)
                .lockedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        escrowAccountRepository.save(escrow);

        contract.setStatus(ContractStatus.ACTIVE);
        if (contract.getDateSignature() == null) {
            contract.setDateSignature(LocalDateTime.now());
        }
        contract.setUpdatedAt(LocalDateTime.now());
        contractRepository.save(contract);

        Wallet clientWallet = walletService.getOrCreateWallet(contract.getClientCin());

        Transaction transaction = Transaction.builder()
                .reference((simulationEnabled ? "TRX-SIM-" : "TRX-") + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .contractId(contract.getId())
                .escrowId(escrow.getId())
                .walletId(clientWallet.getId())
                .type(TransactionType.DEPOSIT)
                .montant(contract.getMontantTotal())
                .methodePaiement(simulationEnabled ? PaymentMethod.WALLET : PaymentMethod.STRIPE)
                .stripePaymentIntentId(paymentIntentId)
                .status(TransactionStatus.PROCESSED)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        log.info("{}Payment confirmed for contract: {}", simulationEnabled ? " SIMULATION: " : "", contractId);
    }

    @Override
    public void releaseApprovedMilestone(Long milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, MILESTONE_NOT_FOUND_MSG));

        Long contractId = milestone.getContractId();
        if (contractId == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Milestone has no contractId: " + milestoneId);
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, CONTRACT_NOT_FOUND_MSG));

        BigDecimal amount = requireMilestoneAmount(milestone, null);
        doRelease(contract, milestone, amount);
    }

    public void releasePaymentToFreelancer(Long contractId, Long milestoneId, BigDecimal amount) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, CONTRACT_NOT_FOUND_MSG));

        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, MILESTONE_NOT_FOUND_MSG));

        if (milestone.getContractId() == null || !milestone.getContractId().equals(contractId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Milestone does not belong to contract. milestoneId="
                    + milestoneId + " contractId=" + contractId + " milestone.contractId=" + milestone.getContractId());
        }

        BigDecimal safeAmount = requireMilestoneAmount(milestone, amount);
        doRelease(contract, milestone, safeAmount);
    }

    private static BigDecimal requireMilestoneAmount(Milestone milestone, BigDecimal explicitAmount) {
        if (milestone.getMontant() == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Milestone amount (montant) is required. milestoneId=" + milestone.getId());
        }
        if (milestone.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Milestone amount must be > 0. milestoneId=" + milestone.getId());
        }

        if (explicitAmount == null) {
            return milestone.getMontant();
        }
        if (explicitAmount.compareTo(milestone.getMontant()) != 0) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Amount must equal milestone amount. milestoneId=" + milestone.getId()
                    + " expected=" + milestone.getMontant() + " provided=" + explicitAmount);
        }
        return explicitAmount;
    }

    private void doRelease(Contract contract, Milestone milestone, BigDecimal amount) {
        assertReleasePreconditions(contract, milestone);
        assertNotAlreadyReleased(milestone.getId());

        EscrowAccount escrow = requireEscrowForRelease(contract.getId());
        assertEscrowCanRelease(escrow, amount);

        ReleaseAmounts amounts = computeReleaseAmounts(contract, amount);
        logReleaseSimulation(contract, milestone, amount, amounts);

        updateEscrowForRelease(escrow, amount);
        creditFreelancerAndPlatform(contract, milestone, escrow, amounts);
        createReleaseTransaction(contract, milestone, escrow, amount, amounts);
        completeContractIfEscrowReleased(contract, escrow);

        log.info("{} Released {} DT to freelancer {} (gross: {}, commission: {})",
                simulationEnabled ? "🔧 SIMULATION:" : "", amounts.netAmount(), contract.getFreelancerCin(), amount, amounts.commission());
    }

    private record ReleaseAmounts(BigDecimal commissionRate, BigDecimal commission, BigDecimal netAmount) {
    }

    private void assertReleasePreconditions(Contract contract, Milestone milestone) {
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Contract is not active. contractId=" + contract.getId() + " status=" + contract.getStatus());
        }
        if (milestone.getStatus() != MilestoneStatus.APPROVED && milestone.getStatus() != MilestoneStatus.AUTO_APPROVED) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Milestone is not approved. milestoneId=" + milestone.getId() + " status=" + milestone.getStatus());
        }
    }

    private void assertNotAlreadyReleased(Long milestoneId) {
        if (transactionRepository.existsByMilestoneIdAndType(milestoneId, TransactionType.RELEASE)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Milestone already released. milestoneId=" + milestoneId);
        }
    }

    private EscrowAccount requireEscrowForRelease(Long contractId) {
        return escrowAccountRepository.findByContractId(contractId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Escrow not found"));
    }

    private static void assertEscrowCanRelease(EscrowAccount escrow, BigDecimal amount) {
        if (escrow.getStatus() == EscrowStatus.DISPUTED) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Escrow is disputed. Payments are frozen until the dispute is resolved.");
        }
        if (escrow.getMontantBloque() == null || escrow.getMontantBloque().compareTo(amount) < 0) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Insufficient escrow balance");
        }
    }

    private static ReleaseAmounts computeReleaseAmounts(Contract contract, BigDecimal amount) {
        BigDecimal commissionRate = contract.getCommissionRate() != null ? contract.getCommissionRate() : BigDecimal.valueOf(10);
        BigDecimal commission = amount.multiply(commissionRate).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal netAmount = amount.subtract(commission);
        if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Net amount cannot be negative. amount=" + amount + " commission=" + commission);
        }
        return new ReleaseAmounts(commissionRate, commission, netAmount);
    }

    private void logReleaseSimulation(Contract contract, Milestone milestone, BigDecimal amount, ReleaseAmounts amounts) {
        if (!simulationEnabled) {
            return;
        }
        log.info(" SIMULATION: Releasing payment to freelancer - Contract: {}, Milestone: {}", contract.getId(), milestone.getId());
        log.info("   Amount: {} (Commission: {}, Net: {})", amount, amounts.commission(), amounts.netAmount());
    }

    private void updateEscrowForRelease(EscrowAccount escrow, BigDecimal amount) {
        escrow.setMontantBloque(escrow.getMontantBloque().subtract(amount));
        escrow.setMontantLibere(escrow.getMontantLibere().add(amount));
        if (escrow.getMontantBloque().compareTo(BigDecimal.ZERO) == 0) {
            escrow.setStatus(EscrowStatus.RELEASED);
            escrow.setReleasedAt(LocalDateTime.now());
        } else {
            escrow.setStatus(EscrowStatus.PARTIALLY_RELEASED);
        }
        escrow.setUpdatedAt(LocalDateTime.now());
        escrowAccountRepository.save(escrow);
    }

    private void creditFreelancerAndPlatform(Contract contract, Milestone milestone, EscrowAccount escrow, ReleaseAmounts amounts) {
        // Créditer le freelancer (net)
        walletService.credit(contract.getFreelancerCin(), amounts.netAmount(),
                (simulationEnabled ? "SIMULATION: " : "") + "Paiement contrat #" + contract.getId() + " - Jalon: " + milestone.getTitre());

        // Créditer la plateforme (commission) - wallet configurable, par défaut id=1
        if (amounts.commission().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Wallet platformWallet = walletRepository.findById(platformWalletId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "Platform wallet not found. walletId=" + platformWalletId));

        walletService.credit(platformWallet.getUserCin(), amounts.commission(),
                (simulationEnabled ? SIMULATION_PREFIX : "") + "Commission contrat #" + contract.getId() + " - Jalon: " + milestone.getTitre());

        processCommission(contract, milestone, escrow, amounts.commission(), amounts.commissionRate());
    }

    private void createReleaseTransaction(Contract contract, Milestone milestone, EscrowAccount escrow, BigDecimal amount, ReleaseAmounts amounts) {
        Wallet freelancerWallet = walletService.getOrCreateWallet(contract.getFreelancerCin());
        Transaction transaction = Transaction.builder()
                .reference((simulationEnabled ? "TRX-SIM-" : "TRX-") + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .contractId(contract.getId())
                .milestoneId(milestone.getId())
                .escrowId(escrow.getId())
                .walletId(freelancerWallet.getId())
                .type(TransactionType.RELEASE)
                .montant(amount)
                .commissionDynamique(amounts.commissionRate())
                .montantCommission(amounts.commission())
                .montantNet(amounts.netAmount())
                .methodePaiement(simulationEnabled ? PaymentMethod.WALLET : PaymentMethod.STRIPE)
                .description("Release to freelancer (net)")
                .status(TransactionStatus.PROCESSED)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);
    }

    private void completeContractIfEscrowReleased(Contract contract, EscrowAccount escrow) {
        if (escrow.getStatus() != EscrowStatus.RELEASED) {
            return;
        }
        contract.setStatus(ContractStatus.COMPLETED);
        contract.setUpdatedAt(LocalDateTime.now());
        contractRepository.save(contract);
    }

    @Override
    public String getPaymentStatus(String paymentIntentId) {
        if (simulationEnabled) {
            log.info(" {}Getting payment status for: {}", SIMULATION_PREFIX, paymentIntentId);

            if (paymentIntentId.startsWith("sim_")) {
                return "succeeded";
            }
            return "pending";
        }

        try {
            var paymentIntent = stripeService.getPaymentIntent(paymentIntentId);
            return paymentIntent.getStatus();
        } catch (com.stripe.exception.StripeException e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Stripe error: " + e.getMessage());
        }
    }

    @Override
    public void refundMilestoneToClient(Long milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, MILESTONE_NOT_FOUND_MSG));

        Long contractId = milestone.getContractId();
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, CONTRACT_NOT_FOUND_MSG));

        BigDecimal amount = requireMilestoneAmount(milestone, null);

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Contract is not active. contractId=" + contract.getId());
        }
        if (milestone.getStatus() != MilestoneStatus.CANCELLED) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Milestone is not cancelled. milestoneId=" + milestone.getId());
        }

        EscrowAccount escrow = escrowAccountRepository.findByContractId(contract.getId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Escrow not found"));

        if (escrow.getMontantBloque() == null || escrow.getMontantBloque().compareTo(amount) < 0) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Insufficient escrow balance for refund");
        }

        // Mettre à jour l'escrow
        escrow.setMontantBloque(escrow.getMontantBloque().subtract(amount));
        escrow.setMontantTotal(escrow.getMontantTotal().subtract(amount)); // Refunding reduces the total project value held
        if (escrow.getMontantBloque().compareTo(BigDecimal.ZERO) == 0 && escrow.getMontantLibere().compareTo(BigDecimal.ZERO) == 0) {
            escrow.setStatus(EscrowStatus.RELEASED);
        }
        escrow.setUpdatedAt(LocalDateTime.now());
        escrowAccountRepository.save(escrow);

        // Rembourser le client
        walletService.credit(contract.getClientCin(), amount,
                (simulationEnabled ? SIMULATION_PREFIX : "") + "Remboursement contrat #" + contract.getId() + " - Jalon annulé: " + milestone.getTitre());

        // Créer la transaction de remboursement
        Wallet clientWallet = walletService.getOrCreateWallet(contract.getClientCin());

        Transaction transaction = Transaction.builder()
                .reference((simulationEnabled ? "TRX-SIM-REF-" : "TRX-REF-") + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .contractId(contract.getId())
                .milestoneId(milestone.getId())
                .escrowId(escrow.getId())
                .walletId(clientWallet.getId())
                .type(TransactionType.REFUND)
                .montant(amount)
                .methodePaiement(simulationEnabled ? PaymentMethod.WALLET : PaymentMethod.STRIPE)
                .description("Remboursement suite à l'annulation du jalon")
                .status(TransactionStatus.PROCESSED)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        log.info("{} Refunded {} DT to client {} for cancelled milestone {}",
                simulationEnabled ? SIMULATION_PREFIX : "", amount, contract.getClientCin(), milestone.getId());
    }

    private void processCommission(Contract contract, Milestone milestone, EscrowAccount escrow, BigDecimal commission, BigDecimal commissionRate) {
        Wallet platformWallet = walletRepository.findById(platformWalletId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Platform wallet not found. walletId=" + platformWalletId));
        Transaction commissionTx = Transaction.builder()
                .reference((simulationEnabled ? "TRX-SIM-COM-" : "TRX-COM-") + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .contractId(contract.getId())
                .milestoneId(milestone.getId())
                .escrowId(escrow.getId())
                .walletId(platformWallet.getId())
                .type(TransactionType.COMMISSION)
                .montant(commission)
                .commissionDynamique(commissionRate)
                .montantCommission(commission)
                .montantNet(commission)
                .methodePaiement(simulationEnabled ? PaymentMethod.WALLET : PaymentMethod.STRIPE)
                .description("Commission plateforme")
                .status(TransactionStatus.PROCESSED)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(commissionTx);
    }
}
