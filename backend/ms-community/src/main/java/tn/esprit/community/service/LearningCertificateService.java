package tn.esprit.community.service;

import tn.esprit.community.dto.lms.CertificateDTO;
import tn.esprit.community.dto.lms.CertificateRequestDTO;

public interface LearningCertificateService {

    CertificateDTO issueCertificate(CertificateRequestDTO request);
}
