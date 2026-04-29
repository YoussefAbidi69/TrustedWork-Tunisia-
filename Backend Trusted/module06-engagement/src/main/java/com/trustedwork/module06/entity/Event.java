package com.trustedwork.module06.entity;

import com.trustedwork.module06.enums.EventStatus;
import com.trustedwork.module06.enums.EventType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private EventType type;

    private String city;
    private String governorate;
    private boolean online;
    private int capacity;
    private int registeredCount;

    @Column(nullable = false)
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Long organizerUserId;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<EventRegistration> registrations = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
}
