package tn.esprit.userservice.service;

import tn.esprit.userservice.dto.KycReviewRequest;
import tn.esprit.userservice.dto.KycSubmitRequest;
import tn.esprit.userservice.dto.UserDTO;

import java.util.List;

public interface IKycService {

    UserDTO submitKyc(Integer cin, KycSubmitRequest request);

    UserDTO reviewKyc(Integer cin, KycReviewRequest request, String adminEmail);

    List<UserDTO> getPendingKycRequests();

    UserDTO getKycStatus(Integer cin);
}