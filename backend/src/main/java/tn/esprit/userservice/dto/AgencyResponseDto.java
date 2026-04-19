package tn.esprit.userservice.dto;

import lombok.*;
import tn.esprit.userservice.entity.AgencyTier;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyResponseDto {
    private Long id;
    private Long creatorId; // Renamed from ownerId
    private String name;
    private String description;
    private String logoUrl;
    private AgencyTier tier;
    private String country;
    private String city;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}