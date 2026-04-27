package tn.esprit.userservice.agency.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyRequestDTO {
    private Long ownerId;
    private String name;
    private String description;
    private String logoUrl;
    private String country;
    private String city;
    private Boolean active;
}
