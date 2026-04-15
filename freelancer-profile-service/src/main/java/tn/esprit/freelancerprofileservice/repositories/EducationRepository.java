package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.freelancerprofileservice.entities.Education;

import java.util.List;

public interface EducationRepository extends JpaRepository<Education, Long> {

    // Récupérer les formations d'un profil, triées par année décroissante
    List<Education> findByProfileIdOrderByGraduationYearDesc(Long profileId);

    // Vérifier si un doublon existe (même diplôme + même institution pour le même profil)
    boolean existsByDegreeIgnoreCaseAndInstitutionIgnoreCaseAndProfileId(
            String degree, String institution, Long profileId);
}