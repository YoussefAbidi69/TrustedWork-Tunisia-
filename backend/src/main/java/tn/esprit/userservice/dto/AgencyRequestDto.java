package tn.esprit.userservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyRequestDto {
    private Long creatorId;
    private String name;
    private String description;
    private String logoUrl;
    private String sector;
    private String website;
    private String country;
    private String city;
    private Boolean active;
}
