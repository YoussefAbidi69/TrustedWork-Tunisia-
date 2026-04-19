package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.AgencyRequestDto;
import tn.esprit.userservice.dto.AgencyResponseDto;
import tn.esprit.userservice.dto.AgencyUpdateDto;
import tn.esprit.userservice.dto.AgencyContextDto;
import tn.esprit.userservice.dto.PublicUserDTO;
import tn.esprit.userservice.entity.Agency;
import tn.esprit.userservice.mapper.AgencyMapper;
import tn.esprit.userservice.mapper.UserMapper;
import tn.esprit.userservice.service.IAgencyServices;

import java.util.List;

@RestController
@RequestMapping("/agencies")
@RequiredArgsConstructor
public class AgencyController {

    private final IAgencyServices agencyService;
    private final AgencyMapper agencyMapper;
    private final UserMapper userMapper;

    // CREATE
    @PostMapping
    public ResponseEntity<?> createAgency(@RequestBody AgencyRequestDto dto) {
        try {
            System.out.println("[AgencyController] POST /agencies - payload: " + dto);
            Agency agency = agencyMapper.toEntity(dto);
            Agency savedAgency = agencyService.createAgency(agency, dto.getCreatorId());
            AgencyResponseDto responseDto = agencyMapper.toResponseDto(savedAgency);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (Exception e) {
            System.err.println("[AgencyController] Erreur lors de la creation de l'agence: ");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message", "Erreur serveur: " + e.getMessage()));
        }
    }

    // GET ALL
    @GetMapping
    public List<AgencyResponseDto> getAllAgencies() {
        return agencyService.getAllAgencies()
                .stream()
                .map(agencyMapper::toResponseDto)
                .toList();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public AgencyResponseDto getAgencyById(@PathVariable Long id) {
        Agency agency = agencyService.getAgencyById(id);
        return agencyMapper.toResponseDto(agency);
    }

    // GET BY CREATOR (Renamed from owner)
    @GetMapping("/creator/{creatorId}")
    public List<AgencyResponseDto> getAgenciesByCreator(@PathVariable Long creatorId) {
        return agencyService.getAgenciesByCreator(creatorId)
                .stream()
                .map(agencyMapper::toResponseDto)
                .toList();
    }

    // UPDATE
    @PutMapping("/{id}")
    public AgencyResponseDto updateAgency(@PathVariable Long id, @RequestBody AgencyUpdateDto dto) {
        Agency existingAgency = agencyService.getAgencyById(id);
        agencyMapper.updateEntityFromDto(dto, existingAgency);
        Agency updatedAgency = agencyService.updateAgency(id, existingAgency);
        return agencyMapper.toResponseDto(updatedAgency);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void deleteAgency(@PathVariable Long id) {
        agencyService.deleteAgency(id);
    }

    // GET MY CONTEXT
    @GetMapping("/my-context/{userId}")
    public AgencyContextDto getMyAgencyContext(@PathVariable Long userId) {
        return agencyService.getMyAgencyContext(userId);
    }

    // GET MY AGENCIES
    @GetMapping("/my-agencies/{userId}")
    public List<AgencyResponseDto> getMyAgencies(@PathVariable Long userId) {
        return agencyService.getMyAgencies(userId)
                .stream()
                .map(agencyMapper::toResponseDto)
                .toList();
    }

    // GET AVAILABLE FREELANCERS
    @GetMapping("/{agencyId}/available-freelancers")
    public List<PublicUserDTO> getAvailableFreelancers(@PathVariable Long agencyId) {
        return agencyService.getAvailableFreelancers(agencyId)
                .stream()
                .map(userMapper::toPublicDto)
                .toList();
    }
}