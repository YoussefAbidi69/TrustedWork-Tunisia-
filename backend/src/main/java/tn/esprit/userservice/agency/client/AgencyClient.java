package tn.esprit.userservice.agency.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.agency.config.FeignJwtInterceptor;
import tn.esprit.userservice.agency.dto.AgencyRequestDTO;
import tn.esprit.userservice.agency.dto.AgencyResponseDTO;
import tn.esprit.userservice.agency.dto.AgencyUpdateDTO;

import java.util.List;

@FeignClient(name = "ms-agency-service", url = "${agency.service.url}", configuration = FeignJwtInterceptor.class)
public interface AgencyClient {

    @PostMapping("/agencies")
    AgencyResponseDTO createAgency(@RequestBody AgencyRequestDTO dto);

    @GetMapping("/agencies")
    List<AgencyResponseDTO> getAllAgencies();

    @GetMapping("/agencies/{id}")
    AgencyResponseDTO getAgencyById(@PathVariable("id") Long id);

    @GetMapping("/agencies/owner/{ownerId}")
    List<AgencyResponseDTO> getAgenciesByOwner(@PathVariable("ownerId") Long ownerId);

    @PutMapping("/agencies/{id}")
    AgencyResponseDTO updateAgency(@PathVariable("id") Long id, @RequestBody AgencyUpdateDTO dto);

    @DeleteMapping("/agencies/{id}")
    void deleteAgency(@PathVariable("id") Long id);
}
