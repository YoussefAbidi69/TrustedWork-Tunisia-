package tn.esprit.userservice.agency.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.agency.dto.AgencyRequestDTO;
import tn.esprit.userservice.agency.dto.AgencyResponseDTO;
import tn.esprit.userservice.agency.dto.AgencyUpdateDTO;
import tn.esprit.userservice.agency.service.AgencyProxyService;

import java.util.List;

@RestController
@RequestMapping("/users/agencies")
@RequiredArgsConstructor
@Tag(name = "User Agencies Proxy", description = "Proxy to ms-agency-service")
public class AgencyProxyController {

    private final AgencyProxyService agencyService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgencyResponseDTO> createAgency(@RequestBody AgencyRequestDTO dto) {
        return ResponseEntity.ok(agencyService.createAgency(dto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgencyResponseDTO>> getAllAgencies() {
        return ResponseEntity.ok(agencyService.getAllAgencies());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgencyResponseDTO> getAgencyById(@PathVariable Long id) {
        return ResponseEntity.ok(agencyService.getAgencyById(id));
    }

    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgencyResponseDTO>> getAgenciesByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(agencyService.getAgenciesByOwner(ownerId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgencyResponseDTO> updateAgency(@PathVariable Long id, @RequestBody AgencyUpdateDTO dto) {
        return ResponseEntity.ok(agencyService.updateAgency(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAgency(@PathVariable Long id) {
        agencyService.deleteAgency(id);
        return ResponseEntity.noContent().build();
    }
}
