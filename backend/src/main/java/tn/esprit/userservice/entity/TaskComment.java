package tn.esprit.userservice.entity;



import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;


@Entity
@Table(name = "task_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime commentedAt;



    @PrePersist
    public void prePersist() {
        if (this.commentedAt == null) {
            this.commentedAt = LocalDateTime.now();
        }
    }

    // Relation ManyToOne avec Task. Un commentaire appartient à une seule tâche, mais une tâche peut avoir plusieurs commentaires.

    @JsonIgnore

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;
}