package tn.esprit.userservice.dto;

import lombok.*;
import tn.esprit.userservice.entity.JoinRequestStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyJoinRequestUpdateDto {
    /** ACCEPTED or DECLINED */
    private JoinRequestStatus status;
}
