package edu.aiplatform.notifications.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="email_deliveries")
public class EmailDelivery {
    public enum Status { PENDING,SENT,FAILED,SKIPPED,DEFERRED }
    @Id public UUID id;
    public UUID notificationId;
    public UUID recipientId;
    @Column(length=40) public String type;
    @Enumerated(EnumType.STRING) @Column(length=16) public Status status;
    public int priority;
    public Instant scheduledFor;
    public int attempts;
    @Column(length=100) public String lastError;
    public Instant createdAt;
    public Instant sentAt;
    public Instant expiresAt;
    public EmailDelivery() { }
}
