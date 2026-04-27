package tn.esprit.userservice.agency.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyResponseDTO {
    private Long id;
    private Long ownerId;
    private String name;
    private String description;
    private String tier;
    private String logoUrl;
    private String country;
    private String city;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
