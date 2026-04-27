package tn.esprit.userservice.service;

import tn.esprit.userservice.entity.Agency;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.dto.AgencyAnalyticsDto;
import tn.esprit.userservice.dto.AgencyContextDto;
import java.util.List;

public interface IAgencyServices {
    Agency createAgency(Agency agency, Long creatorId); // Updated to accept creatorId

    List<Agency> getAllAgencies();

    Agency getAgencyById(Long id);

    List<Agency> getAgenciesByCreator(Long creatorId); // Renamed from getAgenciesByOwner

    void deleteAgency(Long id, Long requestingUserId);

    Agency updateAgency(Long id, Agency updatedAgency, Long requestingUserId);

    AgencyContextDto getMyAgencyContext(Long userId);

    List<User> getAvailableFreelancers(Long agencyId, Long requestingUserId, String skill, String search);

    List<Agency> getMyAgencies(Long userId);

    AgencyAnalyticsDto getAgencyAnalytics(Long agencyId, Long requestingUserId);
}