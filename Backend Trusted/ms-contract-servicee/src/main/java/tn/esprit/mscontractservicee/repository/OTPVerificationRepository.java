package tn.esprit.mscontractservicee.repository;

import tn.esprit.mscontractservicee.entity.OTPVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OTPVerificationRepository extends JpaRepository<OTPVerification, Long> {
}