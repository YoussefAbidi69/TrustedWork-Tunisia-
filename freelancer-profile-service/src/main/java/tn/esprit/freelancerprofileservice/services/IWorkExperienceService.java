package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.entities.WorkExperience;

import java.util.List;

public interface IWorkExperienceService {
    WorkExperience addWorkExperience(Long userId, WorkExperience experience);
    List<WorkExperience> getMyWorkExperiences(Long userId);
    void deleteWorkExperience(Long expId, Long userId);
}