package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.dto.request.UpdateCertificationRequest;
import tn.esprit.freelancerprofileservice.entities.Certification;

import java.util.List;

public interface ICertificationService {

    Certification addCertification(Long userId, Certification certification);

    Certification updateCertification(Long certId, Long userId, UpdateCertificationRequest request);

    List<Certification> getMyCertifications(Long userId);

    List<Certification> getCertificationsByProfileId(Long profileId);

    void deleteCertification(Long certId, Long userId);
}