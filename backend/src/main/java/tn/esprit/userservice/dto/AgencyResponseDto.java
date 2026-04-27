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
    private Long creatorId;
    private String creatorName;
    private String name;
    private String description;
    private String logoUrl;
    private String sector;
    private String website;
    private AgencyTier tier;
    private String country;
    private String city;
    private Boolean active;
    private int memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}