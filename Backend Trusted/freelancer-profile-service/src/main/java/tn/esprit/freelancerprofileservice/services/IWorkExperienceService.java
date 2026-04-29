package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.entities.WorkExperience;

import java.util.List;

public interface IWorkExperienceService {

    WorkExperience addWorkExperience(Long userId, WorkExperience experience);

    WorkExperience updateWorkExperience(Long expId, Long userId, WorkExperience experience);

    List<WorkExperience> getMyWorkExperiences(Long userId);

    WorkExperience getWorkExperienceById(Long expId, Long userId);

    void deleteWorkExperience(Long expId, Long userId);

    Long getTotalExperienceInMonths(Long userId);
}