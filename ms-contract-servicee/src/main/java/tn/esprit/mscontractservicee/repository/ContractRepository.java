package tn.esprit.mscontractservicee.repository;

import tn.esprit.mscontractservicee.entity.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    Page<Contract> findByClientCinOrFreelancerCin(Long clientCin, Long freelancerCin, Pageable pageable);
    Page<Contract> findByClientCin(Long clientCin, Pageable pageable);
    Page<Contract> findByFreelancerCin(Long freelancerCin, Pageable pageable);
    Page<Contract> findByFreelancerCinAndDateSignatureIsNotNull(Long freelancerCin, Pageable pageable);
}
