package edu.aiplatform.notifications.model;

import java.time.Instant;
import java.util.UUID;

public class EmailDelivery {
    public enum Status {
        PENDING,
        SENT,
        FAILED,
        SKIPPED,
        DEFERRED
    }

    public UUID id;
    public UUID notificationId;
    public UUID recipientId;

    public String type;

    public Status status;

    public int priority;
    public Instant scheduledFor;
    public int attempts;

    public String lastError;

    public Instant createdAt;
    public Instant sentAt;
    public Instant expiresAt;

    public EmailDelivery() {}
}
