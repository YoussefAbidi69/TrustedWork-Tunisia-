package tn.esprit.userservice.service;

public interface IEmailService {

    void sendResetPasswordEmail(String to, String resetLink);

    void sendSimpleEmail(String to, String subject, String body);
}