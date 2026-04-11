package tn.esprit.userservice.service;

public interface ITwoFactorService {

    String generateSecret();

    String getQrCodeUri(String secret, String email);

    boolean verifyCode(String secret, String code);

    String setupTwoFactor(Integer cin);

    void confirmTwoFactor(Integer cin, String code);

    void disable2FA(Integer cin);
}