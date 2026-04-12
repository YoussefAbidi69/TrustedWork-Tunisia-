package tn.esprit.mscontractservicee.service.email;

public interface AppEmailService {
    void sendSignatureRequestEmail(String toEmail, String subject, String body);
}

