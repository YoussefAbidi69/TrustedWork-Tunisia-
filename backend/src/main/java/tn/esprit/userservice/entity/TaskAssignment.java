package tn.esprit.userservice.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;


@Entity
@Table(name = "task_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Float completionScore;

    @Column(nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    private LocalDateTime completedAt;



    @PrePersist
    public void prePersist() {
        if (this.assignedAt == null) {
            this.assignedAt = LocalDateTime.now();
        }

        if (this.completionScore == null) {
            this.completionScore = 0f;
        }
    }


    //Relation ManyToOne avec Task. Une tâche peut être assignée à plusieurs membres, et un membre peut être assigné à plusieurs tâches.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    //Relation ManyToOne avec AgencyMember. Un membre peut être assigné à plusieurs tâches, et une tâche peut être assignée à plusieurs membres.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private AgencyMember member;
}