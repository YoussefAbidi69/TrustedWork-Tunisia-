package tn.esprit.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerRecommendationDTO {
    private Long freelancerId;
    private String firstName;
    private String lastName;
    private String email;
    private String headline;
    private String bio;
    private String location;
    private List<String> skills;
    private String availability;
    private Integer trustLevel;
    private String kycStatus;
    private String photo;
    private Float recommendationScore;
    private ScoreBreakdownDTO scoreBreakdown;
    private String explanation;
    private Boolean alreadyInvited;
    private String invitationStatus;
}
