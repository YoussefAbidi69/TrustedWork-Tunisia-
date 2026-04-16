package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.dto.response.CareerPathResponse;

public interface ICareerPathService {

    // Recommander un parcours de carrière basé sur les skills existants
    CareerPathResponse recommendCareerPath(Long userId);
}


