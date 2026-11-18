package edu.aiplatform.notifications.model;

import java.time.Instant;
import java.util.UUID;

public class Notification {
    public UUID id;
    public UUID recipientId;

    public NotificationType type;

    public UUID sourceEventId;

    public String title;

    public String summary;

    public String link;

    public Instant readAt;
    public Instant createdAt;

    public Notification() {}
}
