package edu.aiplatform.notifications.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="notifications")
public class Notification {
    @Id public UUID id;
    public UUID recipientId;
    @Enumerated(EnumType.STRING) @Column(length=40) public NotificationType type;
    public UUID sourceEventId;
    @Column(length=200) public String title;
    @Column(length=500) public String summary;
    @Column(length=500) public String link;
    public Instant readAt;
    public Instant createdAt;
    public Notification() { }
}
