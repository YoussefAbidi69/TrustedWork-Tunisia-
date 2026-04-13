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
    private final IWalletService walletService;
    private final IStripeService stripeService;

    @Value("${payment.simulation.enabled:false}")
    private boolean simulationEnabled;

    @Value("${app.signature.required:false}")
    private boolean signatureRequired;

    @Override
    public PaymentIntentResponse createPaymentIntent(Long contractId, String email) throws Exception {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if (signatureRequired) {
            if (contract.getStatus() != ContractStatus.PENDING_PAYMENT) {
                throw new RuntimeException("Contract cannot be paid until it is signed. Status: " + contract.getStatus());
            }
        } else {
            if (signatureRequired) {
                if (contract.getStatus() != ContractStatus.PENDING_PAYMENT) {
                    throw new RuntimeException("Contract cannot be paid until it is signed. Status: " + contract.getStatus());
                }
            } else {
                if (contract.getStatus() != ContractStatus.DRAFT && contract.getStatus() != ContractStatus.PENDING_PAYMENT) {
                    throw new RuntimeException("Contract cannot be paid. Status: " + contract.getStatus());
                }
            }
        }

        if (simulationEnabled) {
            log.info("🔧 SIMULATION: Creating simulated payment intent for contract: {}", contractId);

            // En simulation, on crée un ID simulé
            String simulatedPaymentId = "sim_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String simulatedClientSecret = "sim_secret_" + UUID.randomUUID().toString().substring(0, 16).toUpperCase();

            return PaymentIntentResponse.builder()
                    .clientSecret(simulatedClientSecret)
                    .paymentIntentId(simulatedPaymentId)
                    .build();
        }

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
    }

    @Override
    public void confirmPayment(String paymentIntentId, Long contractId) throws Exception {

        if (simulationEnabled) {
            log.info("🔧 SIMULATION: Confirming payment for contract: {}", contractId);
            log.info("   Payment Intent ID: {}", paymentIntentId);

            // En simulation, on valide directement sans appeler Stripe
            Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));

            if (contract.getStatus() != ContractStatus.DRAFT && contract.getStatus() != ContractStatus.PENDING_PAYMENT) {
                throw new RuntimeException("Contract cannot be paid. Status: " + contract.getStatus());
            }
            if (escrowAccountRepository.findByContractId(contractId).isPresent()) {
                throw new RuntimeException("Escrow already exists for contract: " + contractId);
            }

            // Débiter le client
            walletService.debit(contract.getClientCin(), contract.getMontantTotal(),
                    "SIMULATION: Paiement contrat #" + contractId);

            // Créer l'escrow
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

            // Mettre à jour le contrat
            contract.setStatus(ContractStatus.ACTIVE);
            if (contract.getDateSignature() == null) {
                contract.setDateSignature(LocalDateTime.now());
            }
            contract.setUpdatedAt(LocalDateTime.now());
            contractRepository.save(contract);

            // Créer la transaction
            Wallet clientWallet = walletService.getOrCreateWallet(contract.getClientCin());

            Transaction transaction = Transaction.builder()
                    .reference("TRX-SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .contractId(contract.getId())
                    .escrowId(escrow.getId())
                    .walletId(clientWallet.getId())
                    .type(TransactionType.DEPOSIT)
                    .montant(contract.getMontantTotal())
                    .methodePaiement(PaymentMethod.WALLET)
                    .stripePaymentIntentId(paymentIntentId)
                    .status(TransactionStatus.PROCESSED)
                    .createdAt(LocalDateTime.now())
                    .processedAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(transaction);

            log.info(" SIMULATION: Payment confirmed for contract: {}", contractId);
            return;
        }

        // Code réel Stripe
        var paymentIntent = stripeService.getPaymentIntent(paymentIntentId);

        if (!"succeeded".equals(paymentIntent.getStatus())) {
            throw new RuntimeException("Payment not succeeded: " + paymentIntent.getStatus());
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if (signatureRequired) {
            if (contract.getStatus() != ContractStatus.PENDING_PAYMENT) {
                throw new RuntimeException("Contract cannot be paid until it is signed. Status: " + contract.getStatus());
            }
        } else {
            if (contract.getStatus() != ContractStatus.DRAFT && contract.getStatus() != ContractStatus.PENDING_PAYMENT) {
                throw new RuntimeException("Contract cannot be paid. Status: " + contract.getStatus());
            }
        }
        if (escrowAccountRepository.findByContractId(contractId).isPresent()) {
            throw new RuntimeException("Escrow already exists for contract: " + contractId);
        }

        walletService.debit(contract.getClientCin(), contract.getMontantTotal(),
                "Paiement contrat #" + contractId);

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
                .reference("TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .contractId(contract.getId())
                .escrowId(escrow.getId())
                .walletId(clientWallet.getId())
                .type(TransactionType.DEPOSIT)
                .montant(contract.getMontantTotal())
                .methodePaiement(PaymentMethod.STRIPE)
                .stripePaymentIntentId(paymentIntentId)
                .status(TransactionStatus.PROCESSED)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        log.info("Payment confirmed for contract: {}", contractId);
    }

    @Override
    public void releaseApprovedMilestone(Long milestoneId) throws Exception {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found"));

        Long contractId = milestone.getContractId();
        if (contractId == null) {
            throw new RuntimeException("Milestone has no contractId: " + milestoneId);
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        BigDecimal amount = requireMilestoneAmount(milestone, null);
        doRelease(contract, milestone, amount);
    }

    @Override
    public void releasePaymentToFreelancer(Long contractId, Long milestoneId, BigDecimal amount) throws Exception {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found"));

        if (milestone.getContractId() == null || !milestone.getContractId().equals(contractId)) {
            throw new RuntimeException("Milestone does not belong to contract. milestoneId="
                    + milestoneId + " contractId=" + contractId + " milestone.contractId=" + milestone.getContractId());
        }

        BigDecimal safeAmount = requireMilestoneAmount(milestone, amount);
        doRelease(contract, milestone, safeAmount);
    }

    private static BigDecimal requireMilestoneAmount(Milestone milestone, BigDecimal explicitAmount) {
        if (milestone.getMontant() == null) {
            throw new RuntimeException("Milestone amount (montant) is required. milestoneId=" + milestone.getId());
        }
        if (milestone.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Milestone amount must be > 0. milestoneId=" + milestone.getId());
        }

        if (explicitAmount == null) {
            return milestone.getMontant();
        }
        if (explicitAmount.compareTo(milestone.getMontant()) != 0) {
            throw new RuntimeException("Amount must equal milestone amount. milestoneId=" + milestone.getId()
                    + " expected=" + milestone.getMontant() + " provided=" + explicitAmount);
        }
        return explicitAmount;
    }

    private void doRelease(Contract contract, Milestone milestone, BigDecimal amount) {
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("Contract is not active. contractId=" + contract.getId()
                    + " status=" + contract.getStatus());
        }
        if (milestone.getStatus() != MilestoneStatus.APPROVED && milestone.getStatus() != MilestoneStatus.AUTO_APPROVED) {
            throw new RuntimeException("Milestone is not approved. milestoneId=" + milestone.getId()
                    + " status=" + milestone.getStatus());
        }
        if (transactionRepository.existsByMilestoneIdAndType(milestone.getId(), TransactionType.RELEASE)) {
            throw new RuntimeException("Milestone already released. milestoneId=" + milestone.getId());
        }

        EscrowAccount escrow = escrowAccountRepository.findByContractId(contract.getId())
                .orElseThrow(() -> new RuntimeException("Escrow not found"));

        if (escrow.getStatus() == EscrowStatus.DISPUTED) {
            throw new RuntimeException("Escrow is disputed. Payments are frozen until the dispute is resolved.");
        }

        if (escrow.getMontantBloque() == null || escrow.getMontantBloque().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient escrow balance");
        }

        BigDecimal commissionRate = contract.getCommissionRate() != null ? contract.getCommissionRate() : BigDecimal.valueOf(10);
        BigDecimal commission = amount.multiply(commissionRate).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal netAmount = amount.subtract(commission);

        if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Net amount cannot be negative. amount=" + amount + " commission=" + commission);
        }

        if (simulationEnabled) {
            log.info(" SIMULATION: Releasing payment to freelancer - Contract: {}, Milestone: {}",
                    contract.getId(), milestone.getId());
            log.info("   Amount: {} (Commission: {}, Net: {})", amount, commission, netAmount);
        }

        // Mettre à jour l'escrow
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

        // Créditer le freelancer (net)
        walletService.credit(contract.getFreelancerCin(), netAmount,
                (simulationEnabled ? "SIMULATION: " : "") + "Paiement contrat #" + contract.getId() + " - Jalon: " + milestone.getTitre());

        // Créer la transaction de libération (gross + commission + net)
        Wallet freelancerWallet = walletService.getOrCreateWallet(contract.getFreelancerCin());

        Transaction transaction = Transaction.builder()
                .reference((simulationEnabled ? "TRX-SIM-" : "TRX-") + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .contractId(contract.getId())
                .milestoneId(milestone.getId())
                .escrowId(escrow.getId())
                .walletId(freelancerWallet.getId())
                .type(TransactionType.RELEASE)
                .montant(amount)
                .commissionDynamique(commissionRate)
                .montantCommission(commission)
                .montantNet(netAmount)
                .methodePaiement(simulationEnabled ? PaymentMethod.WALLET : PaymentMethod.STRIPE)
                .status(TransactionStatus.PROCESSED)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        if (escrow.getStatus() == EscrowStatus.RELEASED) {
            contract.setStatus(ContractStatus.COMPLETED);
            contract.setUpdatedAt(LocalDateTime.now());
            contractRepository.save(contract);
        }

        log.info("{} Released {} DT to freelancer {} (gross: {}, commission: {})",
                simulationEnabled ? "🔧 SIMULATION:" : "", netAmount, contract.getFreelancerCin(), amount, commission);
    }

    @Override
    public String getPaymentStatus(String paymentIntentId) throws Exception {
        if (simulationEnabled) {
            log.info(" SIMULATION: Getting payment status for: {}", paymentIntentId);

            if (paymentIntentId.startsWith("sim_")) {
                return "succeeded";
            }
            return "pending";
        }

        var paymentIntent = stripeService.getPaymentIntent(paymentIntentId);
        return paymentIntent.getStatus();
    }
}
