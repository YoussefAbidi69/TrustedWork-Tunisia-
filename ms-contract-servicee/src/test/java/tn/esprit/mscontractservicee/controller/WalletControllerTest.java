package tn.esprit.mscontractservicee.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import tn.esprit.mscontractservicee.entity.Wallet;
import tn.esprit.mscontractservicee.repository.TransactionRepository;
import tn.esprit.mscontractservicee.service.IWalletService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private IWalletService walletService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletController walletController;

    private Wallet buildWallet(Long cin, BigDecimal balance) {
        return Wallet.builder()
                .id(1L)
                .userCin(cin)
                .balance(balance)
                .totalEarned(BigDecimal.ZERO)
                .totalSpent(BigDecimal.ZERO)
                .totalCommissionPaid(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetWallet() {
        when(walletService.getOrCreateWallet(123L)).thenReturn(buildWallet(123L, new BigDecimal("500")));

        ResponseEntity<?> result = walletController.getWallet(123L);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
    }

    @Test
    void testCreditWallet() {
        Wallet wallet = buildWallet(123L, new BigDecimal("700"));
        when(walletService.credit(eq(123L), any(BigDecimal.class), anyString())).thenReturn(wallet);

        ResponseEntity<?> result = walletController.credit(123L, new BigDecimal("200"));

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void testGetWalletTransactions_Empty() {
        when(walletService.getOrCreateWallet(123L)).thenReturn(buildWallet(123L, BigDecimal.ZERO));
        when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<?> result = walletController.getWalletTransactions(123L);

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void testGetStripeStatus_Success() throws Exception {
        when(walletService.getStripeAccountStatus(123L)).thenReturn("ACTIVE");

        ResponseEntity<?> result = walletController.getStripeStatus(123L);

        assertEquals(200, result.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("ACTIVE", body.get("status"));
    }

    @Test
    void testGetStripeStatus_Error() throws Exception {
        when(walletService.getStripeAccountStatus(123L))
                .thenThrow(new UnsupportedOperationException("Stripe not in simulation mode"));

        ResponseEntity<?> result = walletController.getStripeStatus(123L);

        assertEquals(400, result.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("Stripe not in simulation mode", body.get("error"));
    }

    @Test
    void testCreateStripeAccount_Error() throws Exception {
        when(walletService.createStripeAccount(anyLong(), anyString(), anyString()))
                .thenThrow(new UnsupportedOperationException("Stripe Connect not available"));

        ResponseEntity<?> result = walletController.createStripeAccount(123L, "test@email.com", "TN");

        assertEquals(400, result.getStatusCode().value());
    }
}
