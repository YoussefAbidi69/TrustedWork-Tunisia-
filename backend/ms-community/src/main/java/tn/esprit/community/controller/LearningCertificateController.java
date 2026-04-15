package tn.esprit.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.lms.CertificateDTO;
import tn.esprit.community.dto.lms.CertificateRequestDTO;
import tn.esprit.community.service.LearningCertificateService;

@RestController
@RequestMapping("/api/certificates")
public class LearningCertificateController {

    private final LearningCertificateService learningCertificateService;

    public LearningCertificateController(LearningCertificateService learningCertificateService) {
        this.learningCertificateService = learningCertificateService;
    }

    @PostMapping
    public ResponseEntity<CertificateDTO> issue(@RequestBody CertificateRequestDTO request) {
        return new ResponseEntity<>(learningCertificateService.issueCertificate(request), HttpStatus.CREATED);
    }
}
