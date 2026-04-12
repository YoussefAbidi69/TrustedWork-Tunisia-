package tn.esprit.mscontractservicee.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.mscontractservicee.entity.Wallet;
import tn.esprit.mscontractservicee.repository.WalletRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WalletServiceImpl implements IWalletService {

    private final WalletRepository walletRepository;
    private final IStripeService stripeService;

    @Value("${payment.simulation.enabled:false}")
    private boolean simulationEnabled;

    @Override
    public Wallet getOrCreateWallet(Long userCin) {
        return walletRepository.findByUserCin(userCin)
                .orElseGet(() -> createWallet(userCin));
    }

    @Override
    public Wallet createWallet(Long userCin) {
        Wallet wallet = Wallet.builder()
                .userCin(userCin)
                .balance(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .totalSpent(BigDecimal.ZERO)
                .totalCommissionPaid(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet credit(Long userCin, BigDecimal amount, String description) {
        Wallet wallet = getOrCreateWallet(userCin);
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setTotalEarned(wallet.getTotalEarned().add(amount));
        wallet.setUpdatedAt(LocalDateTime.now());
        log.info("Credited {} DT to userCin {}: {}", amount, userCin, description);
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet debit(Long userCin, BigDecimal amount, String description) {
        Wallet wallet = getOrCreateWallet(userCin);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance. Balance: " + wallet.getBalance());
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setTotalSpent(wallet.getTotalSpent().add(amount));
        wallet.setUpdatedAt(LocalDateTime.now());
        log.info("Debited {} DT from userCin {}: {}", amount, userCin, description);
        return walletRepository.save(wallet);
    }

    // ==================== STRIPE CONNECT (SIMULATION MODE) ====================

    @Override
    public String createStripeAccount(Long userCin, String email, String country) throws Exception {
        if (simulationEnabled) {
            log.info("SIMULATION: Creating simulated Stripe account for userCin: {}", userCin);

            Wallet wallet = getOrCreateWallet(userCin);
            String simulatedAccountId = "acct_sim_" + userCin + "_" + System.currentTimeMillis();

            wallet.setStripeAccountId(simulatedAccountId);
            wallet.setStripeAccountStatus("ACTIVE");
            wallet.setUpdatedAt(LocalDateTime.now());
            walletRepository.save(wallet);

            log.info("SIMULATION: Stripe account created: {}", simulatedAccountId);
            return simulatedAccountId;
        }

        // Real Stripe Connect code disabled in simulation.
        throw new UnsupportedOperationException("Stripe Connect not available in simulation mode");
    }

    @Override
    public String getStripeAccountStatus(Long userCin) throws Exception {
        if (simulationEnabled) {
            Wallet wallet = getOrCreateWallet(userCin);
            if (wallet.getStripeAccountId() == null) {
                return "NOT_CREATED";
            }
            log.info("SIMULATION: Stripe account status for userCin {}: {}", userCin, wallet.getStripeAccountStatus());
            return wallet.getStripeAccountStatus() != null ? wallet.getStripeAccountStatus() : "ACTIVE";
        }

        throw new UnsupportedOperationException("Stripe Connect not available in simulation mode");
    }

    @Override
    public String getOnboardingLink(Long userCin) throws Exception {
        if (simulationEnabled) {
            log.info("SIMULATION: Generating onboarding link for userCin: {}", userCin);
            return "http://localhost:4200/wallet/simulation-success";
        }

        throw new UnsupportedOperationException("Stripe Connect not available in simulation mode");
    }
}

