package com.trustedwork.module06.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class LeaderboardDTO {
    private Long userId;
    private String governorate;
    private double engagementScore;
    private int rank;
    private String firstName;
    private String lastName;
    private String photo;
}
