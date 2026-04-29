package tn.esprit.smartjobboard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_msg_conversation", columnList = "conversationId, createdAt"),
    @Index(name = "idx_msg_receiver_read", columnList = "receiverId, isRead")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String conversationId;

    @Column(nullable = false)
    private Long jobOfferId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private Long receiverId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(length = 1000)
    private String fileUrl;

    @Column(length = 1000)
    private String meetUrl;

    @Column(nullable = false)
    private boolean isRead;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
