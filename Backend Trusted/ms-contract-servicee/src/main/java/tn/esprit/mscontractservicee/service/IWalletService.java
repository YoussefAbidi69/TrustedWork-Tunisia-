package tn.esprit.mscontractservicee.service;

import tn.esprit.mscontractservicee.entity.Wallet;
import java.math.BigDecimal;

public interface IWalletService {

    Wallet getOrCreateWallet(Long userCin);

    Wallet createWallet(Long userCin);

    Wallet credit(Long userCin, BigDecimal amount, String description);

    Wallet debit(Long userCin, BigDecimal amount, String description);

    String createStripeAccount(Long userCin, String email, String country) throws com.stripe.exception.StripeException;

    String getStripeAccountStatus(Long userCin) throws com.stripe.exception.StripeException;

    String getOnboardingLink(Long userCin) throws com.stripe.exception.StripeException;
}
