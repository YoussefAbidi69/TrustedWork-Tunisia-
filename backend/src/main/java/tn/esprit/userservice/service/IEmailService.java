package tn.esprit.userservice.service;

public interface IEmailService {

    void sendResetPasswordEmail(String to, String resetLink);

    void sendSimpleEmail(String to, String subject, String body);

    void sendAutoAssignTaskEmail(String to, String memberFirstName, String agencyName, String taskName, String projectName, String priority, String deadline, String description);
}