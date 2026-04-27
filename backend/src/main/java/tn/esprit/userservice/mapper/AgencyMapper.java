package tn.esprit.userservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.userservice.dto.AgencyRequestDto;
import tn.esprit.userservice.dto.AgencyResponseDto;
import tn.esprit.userservice.dto.AgencyUpdateDto;
import tn.esprit.userservice.entity.Agency;

@Component
public class AgencyMapper {

    public Agency toEntity(AgencyRequestDto dto) {
        if (dto == null) {
            return null;
        }

        // Note: createdBy User relationship should be mapped 
        // in the Service layer where repositories are available.
        return Agency.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .logoUrl(dto.getLogoUrl())
                .sector(dto.getSector())
                .website(dto.getWebsite())
                .country(dto.getCountry())
                .city(dto.getCity())
                .active(dto.getActive())
                .build();
    }

    public AgencyResponseDto toResponseDto(Agency agency) {
        if (agency == null) {
            return null;
        }

        return AgencyResponseDto.builder()
                .id(agency.getId())
                .creatorId(agency.getCreatedBy() != null ? agency.getCreatedBy().getId() : null)
                .creatorName(agency.getCreatedBy() != null
                        ? agency.getCreatedBy().getFirstName() + " " + agency.getCreatedBy().getLastName()
                        : null)
                .name(agency.getName())
                .description(agency.getDescription())
                .logoUrl(agency.getLogoUrl())
                .sector(agency.getSector())
                .website(agency.getWebsite())
                .tier(agency.getTier())
                .country(agency.getCountry())
                .city(agency.getCity())
                .active(agency.getActive())
                .memberCount(agency.getMembers() != null ? agency.getMembers().size() : 0)
                .createdAt(agency.getCreatedAt())
                .updatedAt(agency.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDto(AgencyUpdateDto dto, Agency agency) {
        if (dto == null || agency == null) {
            return;
        }

        if (dto.getName() != null) {
            agency.setName(dto.getName());
        }

        if (dto.getDescription() != null) {
            agency.setDescription(dto.getDescription());
        }

        if (dto.getLogoUrl() != null) {
            agency.setLogoUrl(dto.getLogoUrl());
        }

        if (dto.getSector() != null) {
            agency.setSector(dto.getSector());
        }

        if (dto.getWebsite() != null) {
            agency.setWebsite(dto.getWebsite());
        }

        if (dto.getCountry() != null) {
            agency.setCountry(dto.getCountry());
        }

        if (dto.getCity() != null) {
            agency.setCity(dto.getCity());
        }

        if (dto.getActive() != null) {
            agency.setActive(dto.getActive());
        }
    }
}
