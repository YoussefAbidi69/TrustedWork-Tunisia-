package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.entities.Education;

import java.util.List;

public interface IEducationService {
    Education addEducation(Long userId, Education education);
    List<Education> getMyEducations(Long userId);
    void deleteEducation(Long eduId, Long userId);
}