package tn.esprit.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.userservice.entity.Agency;

import java.util.List;
import java.util.Optional;

public interface IAgencyRepository extends JpaRepository<Agency, Long> {

    List<Agency> findByCreatedById(Long userId);

    Optional<Agency> findByName(String name);

    boolean existsByName(String name);
}