package tn.esprit.mscontractservicee.repository;

import tn.esprit.mscontractservicee.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
}