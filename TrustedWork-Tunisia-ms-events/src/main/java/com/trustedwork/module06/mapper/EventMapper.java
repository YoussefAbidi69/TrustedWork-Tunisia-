package com.trustedwork.module06.mapper;

import com.trustedwork.module06.dto.EventDTO;
import com.trustedwork.module06.entity.Event;

public class EventMapper {
    public static EventDTO toDto(Event ev, java.util.List<Long> userIds) {
        if(ev == null) return null;
        return EventDTO.builder()
                .id(ev.getId())
                .title(ev.getTitle())
                .description(ev.getDescription())
                .type(ev.getType())
                .city(ev.getCity())
                .governorate(ev.getGovernorate())
                .online(ev.isOnline())
                .capacity(ev.getCapacity())
                .registeredCount(ev.getRegisteredCount())
                .startDate(ev.getStartDate())
                .endDate(ev.getEndDate())
                .status(ev.getStatus())
                .registeredUserIds(userIds)
                .build();
    }

    public static Event toEntity(EventDTO dto) {
        if(dto == null) return null;
        return Event.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .type(dto.getType())
                .city(dto.getCity())
                .governorate(dto.getGovernorate())
                .online(dto.isOnline())
                .capacity(dto.getCapacity())
                .registeredCount(dto.getRegisteredCount())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(dto.getStatus())
                .build();
    }
}
