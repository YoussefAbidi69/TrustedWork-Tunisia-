package tn.esprit.mscontractservicee.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.mscontractservicee.dto.WalletResponse;
import tn.esprit.mscontractservicee.dto.WalletTransactionResponse;
import tn.esprit.mscontractservicee.entity.Transaction;
import tn.esprit.mscontractservicee.service.IWalletService;
import tn.esprit.mscontractservicee.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final IWalletService walletService;  //  Interface
    private final TransactionRepository transactionRepository;

    private static WalletResponse toResponse(tn.esprit.mscontractservicee.entity.Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userCin(wallet.getUserCin())
                .balance(wallet.getBalance())
                .totalEarned(wallet.getTotalEarned())
                .totalSpent(wallet.getTotalSpent())
                .totalCommissionPaid(wallet.getTotalCommissionPaid())
                .stripeAccountId(wallet.getStripeAccountId())
                .stripeAccountStatus(wallet.getStripeAccountStatus())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    @GetMapping("/user/{userCin}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long userCin) {
        return ResponseEntity.ok(toResponse(walletService.getOrCreateWallet(userCin)));
    }

    @PostMapping("/user/{userCin}/credit")
    public ResponseEntity<WalletResponse> credit(@PathVariable Long userCin, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(toResponse(walletService.credit(userCin, amount, "Manual credit")));
    }

    @GetMapping("/user/{userCin}/transactions")
    public ResponseEntity<List<WalletTransactionResponse>> getWalletTransactions(@PathVariable Long userCin) {
        var wallet = walletService.getOrCreateWallet(userCin);
        List<Transaction> txs = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
        List<WalletTransactionResponse> res = txs.stream()
                .map(tx -> WalletTransactionResponse.builder()
                        .id(tx.getId())
                        .reference(tx.getReference())
                        .type(tx.getType())
                        .montant(tx.getMontant())
                        .description(tx.getDescription())
                        .status(tx.getStatus())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(res);
    }

    @PostMapping("/stripe/connect/{userCin}")
    public ResponseEntity<Object> createStripeAccount(
            @PathVariable Long userCin,
            @RequestParam String email,
            @RequestParam(defaultValue = "TN") String country) {
        try {
            String accountId = walletService.createStripeAccount(userCin, email, country);
            String link = walletService.getOnboardingLink(userCin);
            Map<String, String> res = new HashMap<>();
            res.put("stripeAccountId", accountId);
            res.put("onboardingLink", link);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stripe/status/{userCin}")
    public ResponseEntity<Object> getStripeStatus(@PathVariable Long userCin) {
        try {
            String status = walletService.getStripeAccountStatus(userCin);
            return ResponseEntity.ok(Map.of("status", status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
