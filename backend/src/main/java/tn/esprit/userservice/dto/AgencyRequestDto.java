package tn.esprit.userservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyRequestDto {
    private Long creatorId; // Renamed from ownerId
    private String name;
    private String description;
    private String logoUrl;
    private String country;
    private String city;
    private Boolean active;
}
