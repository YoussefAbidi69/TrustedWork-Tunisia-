package tn.esprit.userservice.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyAnalyticsDto {
    private long totalTasks;
    private long completedTasks;
    private long cancelledTasks;
    private double averageTaskDays;
    private List<MemberRankingDto> topMembers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberRankingDto {
        private Long memberId;
        private String fullName;
        private String avatarUrl;
        private double averageCompletionScore;
        private long completedTaskCount;
    }
}
