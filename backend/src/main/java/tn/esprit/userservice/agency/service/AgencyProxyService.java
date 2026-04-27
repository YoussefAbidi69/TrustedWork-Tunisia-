package tn.esprit.userservice.agency.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.agency.client.AgencyClient;
import tn.esprit.userservice.agency.dto.AgencyRequestDTO;
import tn.esprit.userservice.agency.dto.AgencyResponseDTO;
import tn.esprit.userservice.agency.dto.AgencyUpdateDTO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgencyProxyService {

    private final AgencyClient agencyClient;

    public AgencyResponseDTO createAgency(AgencyRequestDTO dto) {
        return agencyClient.createAgency(dto);
    }

    public List<AgencyResponseDTO> getAllAgencies() {
        return agencyClient.getAllAgencies();
    }

    public AgencyResponseDTO getAgencyById(Long id) {
        return agencyClient.getAgencyById(id);
    }

    public List<AgencyResponseDTO> getAgenciesByOwner(Long ownerId) {
        return agencyClient.getAgenciesByOwner(ownerId);
    }

    public AgencyResponseDTO updateAgency(Long id, AgencyUpdateDTO dto) {
        return agencyClient.updateAgency(id, dto);
    }

    public void deleteAgency(Long id) {
        agencyClient.deleteAgency(id);
    }
}
