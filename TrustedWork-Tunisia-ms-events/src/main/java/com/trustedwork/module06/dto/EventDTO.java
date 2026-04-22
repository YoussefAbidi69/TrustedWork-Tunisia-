package com.trustedwork.module06.dto;

import com.trustedwork.module06.enums.EventStatus;
import com.trustedwork.module06.enums.EventType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class EventDTO {
    private Long id;
    private String title;
    private String description;
    private EventType type;
    private String city;
    private String governorate;
    private boolean online;
    private int capacity;
    private int registeredCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private EventStatus status;
    private java.util.List<Long> registeredUserIds;
}
