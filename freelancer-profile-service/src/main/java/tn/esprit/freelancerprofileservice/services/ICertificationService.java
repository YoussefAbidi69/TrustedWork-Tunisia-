package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.entities.Certification;

import java.util.List;

public interface ICertificationService {
    Certification addCertification(Long userId, Certification certification);
    List<Certification> getMyCertifications(Long userId);
    void deleteCertification(Long certId, Long userId);
}