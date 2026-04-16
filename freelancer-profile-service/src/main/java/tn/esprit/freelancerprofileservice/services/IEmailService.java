package tn.esprit.freelancerprofileservice.services;

public interface IEmailService {
    void sendReportStatusEmail(String to, String fullName, String status, String description);

    void sendProfileIncompleteReminder(String to, String fullName, int completenessScore);
    void sendCertificationExpiryAlert(String to, String fullName, String certTitle, String expiryDate);
}