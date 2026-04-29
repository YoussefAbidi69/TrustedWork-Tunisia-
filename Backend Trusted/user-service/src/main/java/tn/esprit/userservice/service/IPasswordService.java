package tn.esprit.userservice.service;

public interface IPasswordService {

    String forgotPassword(String email, String frontendUrl);

    void resetPassword(String token, String newPassword);

    void changePassword(String email, String currentPassword, String newPassword);
}