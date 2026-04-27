package tn.esprit.userservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyJoinRequestDto {
    /** ID of the freelancer sending the request */
    private Long requesterId;
    /** Optional message to the agency owner */
    private String message;
}
