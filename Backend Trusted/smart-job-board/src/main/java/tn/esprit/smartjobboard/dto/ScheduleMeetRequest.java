package tn.esprit.smartjobboard.dto;

import lombok.Data;

@Data
public class ScheduleMeetRequest {
    private String conversationId;
    private String title;
    private String date;
    private String time;
    private Integer duration;
    private String note;
}
