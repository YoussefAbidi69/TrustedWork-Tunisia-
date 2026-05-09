package tn.esprit.mscontractservicee.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import tn.esprit.mscontractservicee.enums.NotificationType;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Notification implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "recipient_cin", nullable = false)
    Long recipientCin;

    @Column(nullable = false)
    String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    String message;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    NotificationType type = NotificationType.INFO;

    // L'URL ou la route Angular vers laquelle rediriger (ex: /app/activity/contracts/1)
    String relatedUrl;

    @Builder.Default
    @Column(name = "is_read")
    boolean read = false;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
